package com.mi.onextbox.lsp

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.XmlResourceParser
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.Toast
import com.mi.onextbox.R
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect
import io.github.libxposed.api.XposedInterface
import org.xmlpull.v1.XmlPullParser
import java.lang.ref.WeakReference

/** Rebuilds the removed release UI from the host's own option XMLs. Opening writes nothing. */
internal object GameAssistantDeveloperController {
    private const val TAG = "ONextBox-GameDeveloper"
    private const val ANDROID = "http://schemas.android.com/apk/res/android"
    private var activity = WeakReference<Activity>(null)
    private var dialog: Dialog? = null
    private var choiceDialog: Dialog? = null

    fun hook(loader: ClassLoader) {
        HotReloadLifecycle.register(
            "game-assistant-developer-dialog", capture = { activity.get() },
            canReload = { dialog?.isShowing != true && choiceDialog?.isShowing != true },
            detach = { dismissDialogs(); activity.clear() },
            restore = { state -> activity = WeakReference(state as? Activity) },
        )
        ModernHookRegistry.installCompat("$TAG:resume", Activity::class.java.getDeclaredMethod("onPostResume"),
            object : ModernMethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val host = param.thisObject as? Activity ?: return
                    if (host.packageName == GameAssistantSettings.PACKAGE_NAME) activity = WeakReference(host)
                }
            })
        ModernHookRegistry.installCompat("$TAG:destroy", Activity::class.java.getDeclaredMethod("onDestroy"),
            object : ModernMethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.thisObject !== activity.get()) return
                    dismissDialogs(); activity.clear()
                }
            })
        val owner = ModernReflect.findClass("business.gamespace.service.impl.setting.SettingAssistantImpl", loader)
        val entry = owner.getDeclaredMethod("jumpDeveloperMode")
        ModernHookRegistry.installFast("$TAG:entry", entry, XposedInterface.Hooker { chain ->
            if (!GameAssistantSettings.enabledXposed(GameAssistantSettings.DEVELOPER)) return@Hooker chain.proceed()
            val host = activity.get()?.takeUnless { it.isFinishing || it.isDestroyed } ?: return@Hooker chain.proceed()
            action(host, "entry") { showCategories(host, loader) }
            null
        })
        // The vendor removed the entry body in release builds; ART may have inlined that
        // no-op into the version click handler. Retain the native five-tap counter.
        runCatching { ModernHookRuntime.requireModule().deoptimize(entry) }
        ModernReflect.findClassIfExists("com.nearme.gamespace.desktopspace.setting.ui.a", loader)?.let { fragment ->
            (fragment.declaredMethods.toList() + fragment.declaredClasses.flatMap { it.declaredMethods.toList() })
                .forEach { runCatching { ModernHookRuntime.requireModule().deoptimize(it) } }
        }
        HookLog.i(TAG, "Version-number five-tap developer entry installed")
    }

    private fun showCategories(host: Activity, loader: ClassLoader) {
        val categories = GameAssistantRules.developerCategories.mapNotNull { category ->
            val id = host.resources.getIdentifier("game_develop_options_$category", "xml", host.packageName)
            if (id == 0) null else host.resources.getXml(id).use { xml ->
                while (xml.eventType != XmlPullParser.START_TAG && xml.eventType != XmlPullParser.END_DOCUMENT) xml.next()
                val titleId = xml.getAttributeResourceValue(ANDROID, "title", 0)
                (if (titleId == 0) category else host.getString(titleId)) to id
            }
        }
        require(categories.isNotEmpty()) { "Host developer XMLs are unavailable" }
        val layout = column(host)
        categories.forEach { (title, id) -> layout.addView(Button(host).apply {
            text = title
            setOnClickListener { action(host, "category") { showOptions(host, loader, title, id) } }
        }) }
        show(host, loader, text(host, R.string.game_assistant_developer_title), scroll(host, layout))
    }

    private fun showOptions(host: Activity, loader: ClassLoader, title: String, id: Int) {
        val layout = column(host)
        val prefs = preferences(host, loader)
        var count = 0
        host.resources.getXml(id).use { xml ->
            while (xml.eventType != XmlPullParser.END_DOCUMENT) {
                if (xml.eventType == XmlPullParser.START_TAG) {
                    val key = xml.getAttributeValue(ANDROID, "key")
                    val titleId = xml.getAttributeResourceValue(ANDROID, "title", 0)
                    if (!key.isNullOrBlank() && titleId != 0) {
                        val label = host.getString(titleId)
                        val defaultValue = defaultValue(host, xml)
                        if (xml.name.endsWith("SwitchPreference")) {
                            layout.addView(Switch(host).apply {
                                text = label
                                setPadding(0, dp(host, 10), 0, dp(host, 10))
                                var saved = GameAssistantDeveloperPreferences.boolean(prefs, key, defaultValue == "true")
                                isChecked = saved
                                lateinit var listener: CompoundButton.OnCheckedChangeListener
                                listener = CompoundButton.OnCheckedChangeListener { button, checked ->
                                    if (action(host, "switch:$key") {
                                            check(GameAssistantDeveloperPreferences.saveBoolean(prefs, key, checked)) { "Native preference commit failed" }
                                            saved = checked
                                        }) return@OnCheckedChangeListener
                                    button.setOnCheckedChangeListener(null)
                                    try {
                                        saved = runCatching { GameAssistantDeveloperPreferences.boolean(prefs, key, defaultValue == "true") }
                                            .getOrDefault(saved)
                                        button.isChecked = saved
                                    } finally {
                                        button.setOnCheckedChangeListener(listener)
                                    }
                                }
                                setOnCheckedChangeListener(listener)
                            })
                            count++
                        } else {
                            val entriesId = xml.getAttributeResourceValue(ANDROID, "entries", 0)
                            val valuesId = xml.getAttributeResourceValue(ANDROID, "entryValues", 0)
                            if (entriesId != 0 && valuesId != 0) {
                                val entries = host.resources.getStringArray(entriesId)
                                val values = host.resources.getStringArray(valuesId)
                                if (entries.size == values.size && entries.isNotEmpty()) {
                                    layout.addView(Button(host).apply {
                                        text = label
                                        setOnClickListener { action(host, "choice:$key") { choose(host, label, prefs, key, entries, values, defaultValue) } }
                                    })
                                    count++
                                }
                            }
                        }
                    }
                }
                xml.next()
            }
        }
        require(count > 0) { "No compatible developer options in this category" }
        show(host, loader, title, scroll(host, layout))
    }

    private fun choose(host: Activity, title: String, prefs: SharedPreferences, key: String,
        entries: Array<String>, values: Array<String>, defaultValue: String) {
        val selected = GameAssistantDeveloperPreferences.selection(prefs, key, defaultValue)
        choiceDialog?.dismiss()
        val popup = AlertDialog.Builder(host).setTitle(title).setSingleChoiceItems(entries, values.indexOf(selected)) { choice, index ->
            if (action(host, "selection:$key") {
                    require(index in values.indices) { "Native choice index unavailable" }
                    check(GameAssistantDeveloperPreferences.saveSelection(prefs, key, values[index])) { "Native preference commit failed" }
                }) choice.dismiss()
        }.setNegativeButton(android.R.string.cancel, null).create()
        choiceDialog = popup
        popup.setOnDismissListener { if (choiceDialog === popup) choiceDialog = null }
        popup.show()
    }

    private fun show(host: Activity, loader: ClassLoader, title: String, content: View) {
        dismissDialogs()
        val close = DialogInterface.OnClickListener { popup, _ -> popup.dismiss() }
        // Prefer the exact host's COUI builder, with a stock Android fallback for other versions.
        dialog = runCatching {
            val type = listOf("com.coui.appcompat.dialog.COUIAlertDialogBuilder", "com.coui.appcompat.dialog.e")
                .firstNotNullOfOrNull { ModernReflect.findClassIfExists(it, loader) } ?: error("COUI builder missing")
            val builder = ModernReflect.newInstance(type, host)
            ModernReflect.callMethod(builder, "setTitle", title)
            ModernReflect.callMethod(builder, "setView", content)
            ModernReflect.callMethod(builder, "setPositiveButton", host.getString(android.R.string.ok), close)
            ModernReflect.callMethod(builder, "create") as Dialog
        }.getOrElse { AlertDialog.Builder(host).setTitle(title).setView(content).setPositiveButton(android.R.string.ok, close).create() }
        dialog?.show()
        Toast.makeText(host, text(host, R.string.game_assistant_developer_restart), Toast.LENGTH_LONG).show()
    }

    private fun dismissDialogs() {
        choiceDialog?.dismiss(); choiceDialog = null
        dialog?.dismiss(); dialog = null
    }

    /** No XML, backend or window failure should escape a click callback into the host. */
    private fun action(host: Activity, name: String, block: () -> Unit): Boolean {
        if (host.isFinishing || host.isDestroyed || HotReloadLifecycle.isRetiring ||
            !GameAssistantSettings.enabledXposed(GameAssistantSettings.DEVELOPER)) return false
        return runCatching { block(); true }.onFailure {
            HookLog.w(TAG, "Developer action unavailable: $name", it)
            runCatching { Toast.makeText(host, text(host, R.string.game_assistant_developer_failed), Toast.LENGTH_LONG).show() }
        }.getOrDefault(false)
    }

    private fun defaultValue(host: Activity, xml: XmlResourceParser): String {
        val id = xml.getAttributeResourceValue(ANDROID, "defaultValue", 0)
        if (id == 0) return xml.getAttributeValue(ANDROID, "defaultValue").orEmpty()
        val value = TypedValue()
        host.resources.getValue(id, value, true)
        return value.coerceToString()?.toString().orEmpty()
    }

    private fun column(host: Activity) = LinearLayout(host).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(host, 24), dp(host, 8), dp(host, 24), dp(host, 8))
    }
    private fun scroll(host: Activity, content: View) = ScrollView(host).apply { addView(content) }
    private fun dp(context: Context, value: Int) = (context.resources.displayMetrics.density * value).toInt()
    private fun text(context: Context, id: Int) = context.createPackageContext("com.mi.onextbox", 0).getString(id)
    private fun preferences(host: Activity, loader: ClassLoader): SharedPreferences {
        // SharedDataHelp can delegate to ISharedPreferences. Use its native factory so
        // choices go to the same backing store consumed by SharedPreferencesProxy.
        val factory = ModernReflect.findClassIfExists("e9.a", loader)
            ?: return host.getSharedPreferences(GameAssistantRules.ENV_PREFERENCES, Context.MODE_PRIVATE)
        val getter = factory.declaredMethods.single {
            it.returnType == SharedPreferences::class.java &&
                it.parameterTypes.contentEquals(arrayOf(Context::class.java, String::class.java, Boolean::class.javaPrimitiveType))
        }.apply { isAccessible = true }
        return getter.invoke(ModernReflect.newInstance(factory), host, GameAssistantRules.ENV_PREFERENCES, true) as SharedPreferences
    }
}
