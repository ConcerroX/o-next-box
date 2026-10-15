package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot
import kotlin.math.roundToInt

internal object StatusBarLayoutRules {
    // Match the native C17 order until the user explicitly moves a group.
    val legacyDefaultOrder = listOf("hotspot", "cast", "vpn", "nfc", "alarm_clock", "volume", "bluetooth",
        "location", "rotate", "mute", "network_speed", "mobile", "wifi", "airplane")
    val defaultOrder = listOf("logging", "hotspot", "data_saver", "cast", "vpn", "nfc", "headset", "alarm_clock",
        "volume", "bluetooth", "location", "rotate", "managed_profile", "mute", "network_speed", "zen",
        "mobile", "nosim_all", "wifi", "dynamic_info", "airplane")
    val defaultSimOrder = listOf(0, 1)
    fun group(slot: String) = when (slot) {
        "stacked_mobile", "mobile", "mobile2" -> "mobile"
        else -> slot.takeIf { it in defaultOrder }
    }
    fun normalizeOrder(order: List<String>): List<String> {
        val known = order.filter { it in defaultOrder }.distinct()
        // Old margin-only saves also stored the original fourteen-item order. Expanding
        // the editor must not move previously unknown slots merely because of an upgrade.
        if (known.isEmpty() || known == legacyDefaultOrder) return defaultOrder
        if (known.size == legacyDefaultOrder.size && known.toSet() == legacyDefaultOrder.toSet()) {
            val old = known.iterator()
            return defaultOrder.map { if (it in legacyDefaultOrder) old.next() else it }
        }
        return known + defaultOrder.filterNot { it in known }
    }
    fun normalizeSimOrder(order: List<Int>) = order.filter { it in defaultSimOrder }.distinct().let { known ->
        known + defaultSimOrder.filterNot { it in known }
    }
    /** Match physical slots, never subscription IDs (which change when a SIM is replaced). */
    fun reorderSubscriptions(ids: List<Int>, enabled: Boolean, slots: Map<Int, Int>, order: List<Int>): List<Int> {
        if (!enabled || ids.size < 2) return ids
        val ranks = normalizeSimOrder(order).withIndex().associate { it.value to it.index }
        val known = ids.filter { slots[it] in defaultSimOrder }.sortedBy { ranks.getValue(slots.getValue(it)) }.iterator()
        return ids.map { if (slots[it] in defaultSimOrder) known.next() else it }
    }
    /** Reorder only known slots in their existing positions; keep unknown slots and SIM subslots intact. */
    fun reorder(slots: List<String>, order: List<String>): List<String> {
        val ranks = normalizeOrder(order).withIndex().associate { it.value to it.index }
        val sorted = slots.filter { group(it) != null }.sortedBy { ranks.getValue(group(it)!!) }.iterator()
        return slots.map { if (group(it) == null) it else sorted.next() }
    }
    fun offset(value: Int) = value.coerceIn(-16, 40)
    fun padding(original: Int, offsetDp: Int, density: Float, safeInset: Int): Int {
        if (!density.isFinite() || density <= 0f) return original
        return (original + (offset(offsetDp) * density).roundToInt()).coerceAtLeast(safeInset.coerceAtLeast(0))
    }
    /** Keep a visible native icon row inside the existing bar; never enlarge its window. */
    fun verticalPadding(top: Int, bottom: Int, topDp: Int, bottomDp: Int, density: Float,
        barHeight: Int, iconHeight: Int): Pair<Int, Int> {
        val original = top to bottom
        if (!density.isFinite() || density <= 0f || top < 0 || bottom < 0 || barHeight <= 0 || iconHeight <= 0) return original
        val nativeContent = barHeight.toLong() - top - bottom
        if (nativeContent <= 0) return original
        val budget = barHeight - minOf(iconHeight.toLong(), nativeContent).toInt()
        val nextTop = (top.toDouble() + offset(topDp) * density.toDouble()).roundToInt().coerceAtLeast(0)
        val nextBottom = (bottom.toDouble() + offset(bottomDp) * density.toDouble()).roundToInt().coerceAtLeast(0)
        val total = nextTop.toLong() + nextBottom
        if (total <= budget) return nextTop to nextBottom
        // Share the available padding proportionally when both requested offsets
        // cannot fit. This also permits vertical repositioning without clipping icons.
        val boundedTop = (nextTop.toLong() * budget / total).toInt()
        return boundedTop to (budget - boundedTop)
    }
}

internal object StatusBarLayoutSettings {
    const val ORDER_ENABLED = "statusbar_custom_order_enabled"
    const val ORDER = "statusbar_custom_order"
    const val SIM_ENABLED = "statusbar_custom_sim_order_enabled"
    const val SIM_ORDER = "statusbar_custom_sim_order"
    const val MARGINS_ENABLED = "statusbar_custom_margins_enabled"
    const val LEFT = "statusbar_left_margin_offset"
    const val RIGHT = "statusbar_right_margin_offset"
    const val VERTICAL_MARGINS_ENABLED = "statusbar_custom_vertical_margins_enabled"
    const val TOP = "statusbar_top_margin_offset"
    const val BOTTOM = "statusbar_bottom_margin_offset"
    data class State(val orderEnabled: Boolean = false, val order: List<String> = StatusBarLayoutRules.defaultOrder,
        val marginsEnabled: Boolean = false, val left: Int = 0, val right: Int = 0,
        val simEnabled: Boolean = false, val simOrder: List<Int> = StatusBarLayoutRules.defaultSimOrder,
        val verticalMarginsEnabled: Boolean = false, val top: Int = 0, val bottom: Int = 0)
    fun state(context: Context): State = LspPreferenceStore.prefs(context).let { prefs ->
        State(prefs.getBoolean(ORDER_ENABLED, false), StatusBarLayoutRules.normalizeOrder(prefs.getString(ORDER, "").orEmpty().split(',')),
            prefs.getBoolean(MARGINS_ENABLED, false), StatusBarLayoutRules.offset(prefs.getInt(LEFT, 0)), StatusBarLayoutRules.offset(prefs.getInt(RIGHT, 0)),
            prefs.getBoolean(SIM_ENABLED, false), StatusBarLayoutRules.normalizeSimOrder(prefs.getString(SIM_ORDER, "").orEmpty().split(',').mapNotNull(String::toIntOrNull)),
            prefs.getBoolean(VERTICAL_MARGINS_ENABLED, false), StatusBarLayoutRules.offset(prefs.getInt(TOP, 0)),
            StatusBarLayoutRules.offset(prefs.getInt(BOTTOM, 0)))
    }
    fun orderEnabledXposed() = HookConfigSnapshot.boolean(ORDER_ENABLED, false) == true
    fun orderXposed() = StatusBarLayoutRules.normalizeOrder(HookConfigSnapshot.string(ORDER, "").orEmpty().split(','))
    fun simEnabledXposed() = orderEnabledXposed() && HookConfigSnapshot.boolean(SIM_ENABLED, false) == true
    fun simOrderXposed() = StatusBarLayoutRules.normalizeSimOrder(HookConfigSnapshot.string(SIM_ORDER, "").orEmpty().split(',').mapNotNull(String::toIntOrNull))
    fun marginsXposed() = if (HookConfigSnapshot.boolean(MARGINS_ENABLED, false) == true)
        (HookConfigSnapshot.int(LEFT, 0) ?: 0) to (HookConfigSnapshot.int(RIGHT, 0) ?: 0) else null
    fun verticalMarginsXposed() = if (HookConfigSnapshot.boolean(VERTICAL_MARGINS_ENABLED, false) == true)
        (HookConfigSnapshot.int(TOP, 0) ?: 0) to (HookConfigSnapshot.int(BOTTOM, 0) ?: 0) else null
    fun save(context: Context, value: State) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(ORDER_ENABLED, value.orderEnabled)
            .putString(ORDER, StatusBarLayoutRules.normalizeOrder(value.order).joinToString(","))
            .putBoolean(MARGINS_ENABLED, value.marginsEnabled).putInt(LEFT, StatusBarLayoutRules.offset(value.left))
            .putInt(RIGHT, StatusBarLayoutRules.offset(value.right)).putBoolean(SIM_ENABLED, value.simEnabled)
            .putString(SIM_ORDER, StatusBarLayoutRules.normalizeSimOrder(value.simOrder).joinToString(","))
            .putBoolean(VERTICAL_MARGINS_ENABLED, value.verticalMarginsEnabled)
            .putInt(TOP, StatusBarLayoutRules.offset(value.top)).putInt(BOTTOM, StatusBarLayoutRules.offset(value.bottom)).commit())
        LspConfig.syncReadableState(context)
    }
    fun setOrderEnabled(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(ORDER_ENABLED, enabled).commit())
        LspConfig.syncReadableState(context)
    }
    fun setVerticalMarginsEnabled(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(VERTICAL_MARGINS_ENABLED, enabled).commit())
        LspConfig.syncReadableState(context)
    }
    fun saveOrder(context: Context, order: List<String>) {
        check(LspPreferenceStore.prefs(context).edit().putString(ORDER, StatusBarLayoutRules.normalizeOrder(order).joinToString(",")).commit())
        LspConfig.syncReadableState(context)
    }
    fun saveSimOrder(context: Context, enabled: Boolean, order: List<Int>) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(SIM_ENABLED, enabled)
            .putString(SIM_ORDER, StatusBarLayoutRules.normalizeSimOrder(order).joinToString(",")).commit())
        LspConfig.syncReadableState(context)
    }
    fun resetOrder(context: Context) {
        // Restore only ordering; preserve the master, margins and other features.
        check(LspPreferenceStore.prefs(context).edit().remove(ORDER).remove(SIM_ORDER).remove(SIM_ENABLED).commit())
        LspConfig.syncReadableState(context)
    }
}
