package com.mi.onextbox.lsp

import android.content.Context
import android.os.SystemClock
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal object LauncherGridSettings {
    const val CONFIG = "launcher_custom_grid_config"
    const val REQUEST = "launcher_custom_grid_apply_request"

    fun state(context: Context): LauncherGridState =
        LauncherGridRules.parse(LspPreferenceStore.prefs(context).all[CONFIG] as? String)

    fun stateXposed(): LauncherGridState = LauncherGridRules.parse(HookConfigSnapshot.string(CONFIG, null))

    fun save(context: Context, state: LauncherGridState) {
        val editor = LspPreferenceStore.prefs(context).edit().putString(CONFIG, LauncherGridRules.encode(state))
        if (state.enabled) editor.putString(REQUEST, LauncherGridApplyRequest.create(state.grid, SystemClock.elapsedRealtime()).encode())
        else editor.remove(REQUEST)
        check(editor.commit())
        LspConfig.syncReadableState(context)
    }
}
