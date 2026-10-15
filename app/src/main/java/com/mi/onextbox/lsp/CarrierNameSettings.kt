package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal object CarrierNameSettings {
    const val ENABLED = "systemui_custom_carrier_name_enabled"
    const val SIM1 = "systemui_custom_carrier_name_sim1"
    const val SIM2 = "systemui_custom_carrier_name_sim2"
    val keys = setOf(ENABLED, SIM1, SIM2)
    data class State(val enabled: Boolean = false, val sim1: String = "", val sim2: String = "") {
        fun name(slot: Int) = when (slot) { 0 -> sim1; 1 -> sim2; else -> "" }
    }
    fun state(context: Context): State = LspPreferenceStore.prefs(context).let {
        State(it.getBoolean(ENABLED, false), CarrierNameRules.normalize(it.getString(SIM1, "")),
            CarrierNameRules.normalize(it.getString(SIM2, "")))
    }
    fun enabledXposed() = HookConfigSnapshot.boolean(ENABLED, false) == true
    fun nameXposed(slot: Int): String = when (slot) {
        0 -> CarrierNameRules.normalize(HookConfigSnapshot.string(SIM1, ""))
        1 -> CarrierNameRules.normalize(HookConfigSnapshot.string(SIM2, ""))
        else -> ""
    }
    fun setEnabled(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(ENABLED, enabled).commit())
        LspConfig.syncReadableState(context)
    }
    fun setName(context: Context, slot: Int, name: String) {
        val key = when (slot) { 0 -> SIM1; 1 -> SIM2; else -> error("Unsupported SIM slot") }
        check(LspPreferenceStore.prefs(context).edit().putString(key, CarrierNameRules.normalize(name)).commit())
        LspConfig.syncReadableState(context)
    }
    fun resetNames(context: Context) {
        check(LspPreferenceStore.prefs(context).edit().remove(SIM1).remove(SIM2).commit())
        LspConfig.syncReadableState(context)
    }
}
