package com.mi.onextbox.lsp

import android.content.Context

/** Explicit, default-off consent shared with the running module through modern preferences. */
object ModuleHotReloadSettings {
    const val PREFERENCE_KEY = "module_hot_reload_enabled"

    fun isEnabled(context: Context): Boolean =
        LspPreferenceStore.prefs(context).getBoolean(PREFERENCE_KEY, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        ModernXposedPreferenceSync.setBooleanAndSync(context, PREFERENCE_KEY, enabled)
    }
}
