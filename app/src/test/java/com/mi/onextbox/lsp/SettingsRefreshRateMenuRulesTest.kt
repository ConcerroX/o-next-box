package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class SettingsRefreshRateMenuRulesTest {
    private val current = SettingsRefreshRateMenuRules.Mode(1440, 3168, 144f)
    private fun mode(hz: Float, width: Int = 1440, height: Int = 3168) =
        SettingsRefreshRateMenuRules.Mode(width, height, hz)

    @Test fun real90ModeAtCurrentResolutionIsSupported() {
        assertTrue(SettingsRefreshRateMenuRules.supports90(current, listOf(mode(60f), mode(90f), current)))
        assertTrue(SettingsRefreshRateMenuRules.supports90(current, listOf(mode(89.999f))))
    }

    @Test fun otherResolutionOrRefreshRateDoesNotInvent90() {
        assertFalse(SettingsRefreshRateMenuRules.supports90(current, listOf(mode(90f, 1080, 2376))))
        assertFalse(SettingsRefreshRateMenuRules.supports90(current, listOf(mode(60f), mode(120f), current)))
        assertFalse(SettingsRefreshRateMenuRules.supports90(current, listOf(mode(90.5f), mode(Float.NaN))))
        assertFalse(SettingsRefreshRateMenuRules.supports90(current, emptyList()))
        assertFalse(SettingsRefreshRateMenuRules.supports90(mode(90f, 0, 0), listOf(mode(90f, 0, 0))))
    }

    @Test fun originalNativeChoicesAndTheirOrderArePreserved() {
        val result = SettingsRefreshRateMenuRules.expose90(
            listOf("默认高刷 120 Hz", "极速高刷 144 Hz"), listOf("120", "144"), "90 Hz",
        )!!
        assertEquals(listOf("90 Hz", "默认高刷 120 Hz", "极速高刷 144 Hz"), result.labels)
        assertEquals(listOf("90", "120", "144"), result.values)
    }

    @Test fun anotherExtremeRateKeepsItsNativeLabelAndValue() {
        val result = SettingsRefreshRateMenuRules.expose90(
            listOf("120 localized", "185 localized"), listOf("120", "185"), "90 localized",
        )!!
        assertEquals("185 localized", result.labels.last())
        assertEquals("185", result.values.last())
    }

    @Test fun duplicateUnsupportedAndMalformedMenusAreUntouched() {
        assertNull(SettingsRefreshRateMenuRules.expose90(listOf("90", "120"), listOf("90", "120"), "90 Hz"))
        assertNull(SettingsRefreshRateMenuRules.expose90(listOf("60"), listOf("60"), "90 Hz"))
        assertNull(SettingsRefreshRateMenuRules.expose90(listOf("120", "144"), listOf("120"), "90 Hz"))
        assertNull(SettingsRefreshRateMenuRules.expose90(emptyList(), emptyList(), "90 Hz"))
        assertNull(SettingsRefreshRateMenuRules.expose90(listOf("120"), listOf("120"), ""))
    }
}
