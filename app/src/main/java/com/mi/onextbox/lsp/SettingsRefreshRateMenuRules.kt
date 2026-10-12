package com.mi.onextbox.lsp

import kotlin.math.abs

/** Native rate indices are not Hz or Display.Mode IDs. Never invent an unsupported mode. */
internal object SettingsRefreshRateMenuRules {
    const val NATIVE_90_INDEX = 1
    const val RATE_90 = 90
    const val VALUE_90 = "90"

    data class Mode(val width: Int, val height: Int, val refreshRate: Float)
    data class Entries(val labels: List<CharSequence>, val values: List<CharSequence>)

    fun supports90(current: Mode, supported: List<Mode>): Boolean =
        current.width > 0 && current.height > 0 && supported.any {
            it.width == current.width && it.height == current.height &&
                abs(it.refreshRate - RATE_90) < 0.5f
        }

    /** Insert before 120 Hz, preserving OEM labels, ordering and the extreme confirmation value. */
    fun expose90(
        labels: List<CharSequence>,
        values: List<CharSequence>,
        native90Label: String,
    ): Entries? {
        if (labels.size != values.size || labels.isEmpty() || native90Label.isBlank()) return null
        if (values.any { it.toString() == VALUE_90 }) return null
        val position = values.indexOfFirst { it.toString() == "120" }
        if (position < 0) return null
        return Entries(
            labels.toMutableList().apply { add(position, native90Label) },
            values.toMutableList().apply { add(position, VALUE_90) },
        )
    }
}
