package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal object DataSimSignalSettings {
    const val KEY = "systemui_only_data_sim_signal"
    fun enabled(context: Context) = LspPreferenceStore.prefs(context).getBoolean(KEY, false)
    fun enabledXposed() = HookConfigSnapshot.boolean(KEY, false) == true
    fun set(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(KEY, enabled).commit())
        LspConfig.syncReadableState(context)
    }
}

internal object DataSimSignalRules {
    fun select(ids: List<Int>, enabled: Boolean, dataSubId: Int): List<Int> =
        if (enabled && ids.size > 1 && dataSubId >= 0 && dataSubId in ids) listOf(dataSubId) else ids
}
