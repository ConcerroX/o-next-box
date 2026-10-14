package com.mi.onextbox.lsp

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.view.View
import android.widget.Button
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

    fun hook(loader: ClassLoader) {
        HotReloadLifecycle.register(
            "game-assistant-developer-dialog", capture = { activity.get() },
            canReload = { dialog?.isShowing != true },
            detach = { dialog?.dismiss(); dialog = null; activity.clear() },
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
                    dialog?.dismiss(); dialog = null; activity.clear()
                }
            })
        val owner = ModernReflect.findClass("business.gamespace.service.impl.setting.SettingAssistantImpl", loader)
        val entry = owner.getDeclaredMethod("jumpDeveloperMode")
        ModernHookRegistry.installFast("$TAG:entry", entry, XposedInterface.Hooker { chain ->
            if (!GameAssistantSettings.enabledXposed(GameAssistantSettings.DEVELOPER)) return@Hooker chain.proceed()
            val host = activity.get()?.takeUnless { it.isFinishing || it.isDestroyed } ?: return@Hooker chain.proceed()
            runCatching { showCategories(host, loader) }.onFailure { HookLog.w(TAG, "Developer options unavailable", it) }
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
            setOnClickListener { showOptions(host, loader, title, id) }
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
                        val defaultValue = xml.getAttributeValue(ANDROID, "defaultValue").orEmpty()
                        if (xml.name.endsWith("SwitchPreference")) {
                            layout.addView(Switch(host).apply {
                                text = label
                                setPadding(0, dp(host, 10), 0, dp(host, 10))
                                isChecked = (prefs.all[key] as? Boolean) ?: (defaultValue == "true")
                                setOnCheckedChangeListener { _, checked ->
                                    if (!prefs.edit().putBoolean(key, checked).commit()) {
                                        setOnCheckedChangeListener(null)
                                        isChecked = (prefs.all[key] as? Boolean) ?: (defaultValue == "true")
                                        Toast.makeText(host, text(host, R.string.enabled_features_save_failed), Toast.LENGTH_LONG).show()
                                    }
                                }
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
                                        setOnClickListener { choose(host, label, prefs, key, entries, values, defaultValue) }
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
        val selected = (prefs.all[key] ?: defaultValue).toString()
        AlertDialog.Builder(host).setTitle(title).setSingleChoiceItems(entries, values.indexOf(selected)) { choice, index ->
            val value = values[index]
            val editor = prefs.edit()
            if (GameAssistantRules.developerIntKey(key)) {
                val number = value.toIntOrNull() ?: return@setSingleChoiceItems
                editor.putInt(key, number)
            } else editor.putString(key, value)
            if (!editor.commit()) Toast.makeText(host, text(host, R.string.enabled_features_save_failed), Toast.LENGTH_LONG).show()
            choice.dismiss()
        }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun show(host: Activity, loader: ClassLoader, title: String, content: View) {
        dialog?.dismiss()
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
