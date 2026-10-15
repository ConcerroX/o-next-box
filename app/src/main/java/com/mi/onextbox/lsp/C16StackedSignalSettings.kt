package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal object C16StackedSignalSettings {
    const val KEY = "systemui_c16_stacked_signal"
    fun enabled(context: Context) = LspPreferenceStore.prefs(context).getBoolean(KEY, false)
    fun enabledXposed() = HookConfigSnapshot.boolean(KEY, false) == true
    fun set(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(KEY, enabled).commit())
        LspConfig.syncReadableState(context)
    }
}
