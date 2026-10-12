package com.mi.onextbox.lsp

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.mi.onextbox.R
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

internal class SoftwareUpdateMenuController(resourceKey: String) {
    private val menus = WeakHashMap<Activity, Menu>()
    private val localEntryStates = WeakHashMap<Activity, Boolean>()
    private val dialogs = WeakHashMap<Activity, AlertDialog>()
    private val tasks = ConcurrentHashMap<CancellationSignal, Boolean>()
    private val main = Handler(Looper.getMainLooper())

    init {
        HotReloadLifecycle.register(resourceKey,
            capture = {
                menus.entries.map { (activity, menu) ->
                    arrayOf(activity, menu, dialogs[activity]?.isShowing == true, localEntryStates[activity])
                }.toTypedArray()
            },
            canReload = { tasks.isEmpty() },
            detach = {
                main.removeCallbacksAndMessages(null)
                menus.values.forEach { it.removeItem(DETAILS_ID) }
                dialogs.keys.toList().forEach(::dismiss)
                menus.clear()
                localEntryStates.clear()
            },
            restore = { state ->
                (state as? Array<*>)?.forEach { item ->
                    val row = item as? Array<*> ?: return@forEach
                    val activity = row.getOrNull(0) as? Activity ?: return@forEach
                    val menu = row.getOrNull(1) as? Menu ?: return@forEach
                    if (activity.isFinishing || activity.isDestroyed) return@forEach
                    bind(activity, menu)
                    if (row.getOrNull(3) != localEntryEnabled()) activity.invalidateOptionsMenu()
                    if (row.getOrNull(2) == true && enabled()) show(activity)
                }
            },
        )
    }

    fun bind(activity: Activity, menu: Menu) {
        menus[activity]?.takeIf { it !== menu }?.removeItem(DETAILS_ID)
        menus[activity] = menu
        localEntryStates[activity] = localEntryEnabled()
        menu.removeItem(DETAILS_ID)
        if (!enabled()) {
            dismiss(activity)
            return
        }
        menu.add(Menu.NONE, DETAILS_ID, Menu.NONE, text(activity, R.string.ota_details_menu))
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
            .setOnMenuItemClickListener {
                HotReloadLifecycle.withHook(fallback = { false }) {
                    if (enabled() && !activity.isFinishing && !activity.isDestroyed) {
                        runCatching { show(activity) }
                            .onFailure { HookLog.w("SoftwareUpdate", "Package details unavailable", it) }
                    } else activity.invalidateOptionsMenu()
                    true
                }
            }
    }

    fun remove(activity: Activity) {
        menus.remove(activity)?.removeItem(DETAILS_ID)
        localEntryStates.remove(activity)
        dismiss(activity)
    }

    fun refresh(activity: Activity) {
        val menu = menus[activity] ?: return
        if (localEntryStates[activity] != localEntryEnabled()) {
            activity.invalidateOptionsMenu()
        } else if ((menu.findItem(DETAILS_ID) != null) != enabled()) {
            bind(activity, menu)
        }
    }

    private fun dismiss(activity: Activity) {
        dialogs.remove(activity)?.let { dialog ->
            dialog.setOnShowListener(null)
            dialog.setOnCancelListener(null)
            dialog.setOnDismissListener(null)
            dialog.dismiss()
        }
    }

    private fun show(activity: Activity) {
        if (dialogs[activity]?.isShowing == true) return
        val module = activity.createPackageContext("com.mi.onextbox", Context.CONTEXT_IGNORE_SECURITY)
            .createConfigurationContext(activity.resources.configuration)
        val content = TextView(activity).apply {
            text = module.getString(R.string.ota_details_loading)
            textSize = 14f
            setTextIsSelectable(true)
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, padding / 2)
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle(module.getString(R.string.ota_details_menu))
            .setView(ScrollView(activity).apply { addView(content) })
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(module.getString(R.string.ota_copy_details), null)
            .setNeutralButton(module.getString(R.string.ota_copy_links), null)
            .create()
        dialogs[activity] = dialog
        dialog.setOnDismissListener { dialogs.remove(activity, dialog) }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
        val activityRef = WeakReference(activity)
        val cancellation = CancellationSignal()
        tasks[cancellation] = true
        val context = activity.applicationContext
        try {
            Thread({
                val result = runCatching { SoftwareUpdatePackageReader.read(context, cancellation) }
                val delivered = main.post {
                    tasks.remove(cancellation)
                    HotReloadLifecycle.withHook(fallback = {}) {
                        val owner = activityRef.get() ?: return@withHook
                        if (owner.isFinishing || owner.isDestroyed || dialogs[owner] !== dialog || !dialog.isShowing) return@withHook
                        val packages = result.getOrNull()
                        if (packages == null) {
                            content.text = module.getString(R.string.ota_details_failed)
                        } else if (packages.isEmpty()) {
                            content.text = module.getString(R.string.ota_details_empty)
                        } else {
                            val details = format(owner, module, packages)
                            val links = packages.flatMap { it.links }.distinct().joinToString("\n")
                            content.text = details
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                                isEnabled = true
                                setOnClickListener {
                                    HotReloadLifecycle.withHook(fallback = {}) { copy(owner, module, details) }
                                }
                            }
                            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).apply {
                                isEnabled = links.isNotEmpty()
                                setOnClickListener {
                                    HotReloadLifecycle.withHook(fallback = {}) { copy(owner, module, links) }
                                }
                            }
                        }
                    }
                }
                if (!delivered) tasks.remove(cancellation)
            }, "ONextBox-OtaDetails").start()
        } catch (error: Throwable) {
            tasks.remove(cancellation)
            dismiss(activity)
            throw error
        }
    }

    private fun format(context: Context, module: Context, packages: List<SoftwareUpdatePackageInfo>): String =
        packages.joinToString("\n\n──────────\n\n") { item ->
            listOf(
                module.getString(R.string.ota_details_name, item.name),
                module.getString(R.string.ota_details_version, item.version),
                module.getString(R.string.ota_details_type, item.type),
                module.getString(R.string.ota_details_size, Formatter.formatFileSize(context, item.bytes)),
                module.getString(R.string.ota_details_checksum, item.checksum.ifEmpty { "—" }),
                module.getString(R.string.ota_details_links, item.links.joinToString("\n").ifEmpty { "—" }),
            ).joinToString("\n")
        } + "\n\n" + module.getString(R.string.ota_details_auth_notice)

    private fun copy(context: Context, module: Context, value: String) {
        runCatching {
            context.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText(module.getString(R.string.ota_details_menu), value))
            Toast.makeText(context, module.getString(R.string.ota_details_copied), Toast.LENGTH_SHORT).show()
        }.onFailure { HookLog.w("SoftwareUpdate", "Clipboard unavailable") }
    }

    private fun enabled() = SoftwareUpdateSettings.enabledXposed(SoftwareUpdateFeature.PackageDetails)
    private fun localEntryEnabled() = SoftwareUpdateSettings.enabledXposed(SoftwareUpdateFeature.ShowLocalInstall)
    private fun text(context: Context, id: Int): String =
        context.createPackageContext("com.mi.onextbox", Context.CONTEXT_IGNORE_SECURITY).getString(id)

    private companion object { const val DETAILS_ID = 0x6f4f5441 }
}
