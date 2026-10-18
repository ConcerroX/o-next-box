package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal open class VisualCleanupSetting(val KEY: String) {
    fun enabled(context: Context) = LspPreferenceStore.prefs(context).getBoolean(KEY, false)
    fun enabledXposed() = HookConfigSnapshot.boolean(KEY, false) == true
    fun set(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(KEY, enabled).commit())
        LspConfig.syncReadableState(context)
    }
}

internal object HideScreenshotFloatingSettings : VisualCleanupSetting("screenshot_exclude_floating_apps")

internal object ScreenshotFloatingRules {
    fun exclude(mode: Int, type: Int, packageName: String?): Boolean {
        // Zoom mode is verified in the host OplusWindowInfoExt; do not confuse
        // flexible/embedded split-screen modes (6/120) with floating applications.
        if (type in 1..1999 && mode in setOf(2, 5, 100)) return true
        return type == 2038 && !packageName.isNullOrBlank() &&
            packageName !in setOf("android", "com.android.systemui", "com.oplus.screenshot")
    }
}
