package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal object QsTileCornerRules {
    const val DEFAULT_DP = 24
    const val MAX_DP = 64
    fun normalize(value: Int) = value.coerceIn(0, MAX_DP)
    fun pixels(value: Int, density: Float, width: Int = 0, height: Int = 0): Float? {
        if (!density.isFinite() || density <= 0f) return null
        val radius = normalize(value) * density
        if (!radius.isFinite()) return null
        return if (width > 0 && height > 0) minOf(radius, minOf(width, height) / 2f) else radius
    }
}

internal object QsTileCornerSettings {
    const val ENABLED = "systemui_qs_tile_corner_enabled"
    const val RADIUS = "systemui_qs_tile_corner_radius"
    val keys = setOf(ENABLED, RADIUS)
    data class State(val enabled: Boolean = false, val radius: Int = QsTileCornerRules.DEFAULT_DP)
    fun state(context: Context): State = LspPreferenceStore.prefs(context).let {
        State(it.getBoolean(ENABLED, false), QsTileCornerRules.normalize(it.getInt(RADIUS, QsTileCornerRules.DEFAULT_DP)))
    }
    fun radiusXposed(): Int? = if (HookConfigSnapshot.boolean(ENABLED, false) == true)
        QsTileCornerRules.normalize(HookConfigSnapshot.int(RADIUS, QsTileCornerRules.DEFAULT_DP) ?: QsTileCornerRules.DEFAULT_DP) else null
    fun setEnabled(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(ENABLED, enabled).commit())
        LspConfig.syncReadableState(context)
    }
    fun setRadius(context: Context, radius: Int) {
        check(LspPreferenceStore.prefs(context).edit().putInt(RADIUS, QsTileCornerRules.normalize(radius)).commit())
        LspConfig.syncReadableState(context)
    }
}
