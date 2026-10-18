package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot
import java.util.Locale
import kotlin.math.abs

internal object PowerIndicatorSettings {
    const val PREFIX = "statusbar_power_"
    const val ENABLED = "${PREFIX}enabled"
    data class State(val enabled: Boolean = false, val interval: Int = 2000,
        val decimals: Int = 1, val scale: Int = 0, val source: Int = 0,
        val hideCharging: Boolean = true, val fontSize: Int = 10)
    fun state(context: Context): State {
        val p = LspPreferenceStore.prefs(context)
        return normalize(State(p.getBoolean(ENABLED, false), p.getInt("${PREFIX}interval", 2000),
            p.getInt("${PREFIX}decimals", 1), p.getInt("${PREFIX}scale", 0), p.getInt("${PREFIX}source", 0),
            p.getBoolean("${PREFIX}hide_charging", true), p.getInt("${PREFIX}font", 10)))
    }
    fun snapshot() = normalize(State(HookConfigSnapshot.boolean(ENABLED, false) == true,
        HookConfigSnapshot.int("${PREFIX}interval", 2000) ?: 2000,
        HookConfigSnapshot.int("${PREFIX}decimals", 1) ?: 1,
        HookConfigSnapshot.int("${PREFIX}scale", 0) ?: 0,
        HookConfigSnapshot.int("${PREFIX}source", 0) ?: 0,
        HookConfigSnapshot.boolean("${PREFIX}hide_charging", true) != false,
        HookConfigSnapshot.int("${PREFIX}font", 10) ?: 10))
    fun normalize(s: State) = s.copy(interval = s.interval.coerceIn(500, 10000), decimals = s.decimals.coerceIn(0, 2),
        scale = s.scale.coerceIn(0, 2), source = s.source.coerceIn(0, 2), fontSize = s.fontSize.coerceIn(8, 16))
    fun setEnabled(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(ENABLED, enabled).commit())
        LspConfig.syncReadableState(context)
    }
    fun save(context: Context, value: State) {
        val s = normalize(value)
        check(LspPreferenceStore.prefs(context).edit().putBoolean(ENABLED, s.enabled)
            .putInt("${PREFIX}interval", s.interval).putInt("${PREFIX}decimals", s.decimals)
            .putInt("${PREFIX}scale", s.scale).putInt("${PREFIX}source", s.source)
            .putBoolean("${PREFIX}hide_charging", s.hideCharging).putInt("${PREFIX}font", s.fontSize).commit())
        LspConfig.syncReadableState(context)
    }
}

internal object PowerIndicatorRules {
    // API current: microamps; explicit vendor sysfs mode may be milliamps. Never guess from magnitude.
    fun watts(current: Long, voltageMv: Long, source: Int, scale: Int): Double? {
        if (current == Long.MIN_VALUE || current == Int.MIN_VALUE.toLong() || current == Long.MAX_VALUE ||
            voltageMv !in 2000..20000) return null
        val amps = abs(current.toDouble()) / if (source == 1) 1000.0 else 1_000_000.0
        val multiplier = when (scale) { 1 -> 2.0; 2 -> 0.5; else -> 1.0 }
        return (amps * voltageMv / 1000.0 * multiplier).takeIf { it.isFinite() && it in 0.0..200.0 }
    }
    fun format(watts: Double, decimals: Int, charging: Boolean): String =
        (if (charging) "+" else "") + String.format(Locale.ROOT, "%.${decimals.coerceIn(0, 2)}f W", watts)
}
