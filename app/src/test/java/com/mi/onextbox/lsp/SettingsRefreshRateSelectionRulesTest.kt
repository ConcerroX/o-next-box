package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class SettingsRefreshRateSelectionRulesTest {
    private fun initialize(mode: Int, choice: Int = 144, wasEnabled: Boolean = false, enabled: Boolean = true) =
        SettingsRefreshRateSelectionRules.initializeOnEnable(wasEnabled, enabled, mode, choice)

    @Test fun defaultHighDoesNotAdoptAStaleExtremeChoice() {
        assertEquals(SettingsRefreshRateSelectionRules.Initialization(120, null), initialize(3))
        assertNull(initialize(3, 120))
    }

    @Test fun oldExtremeModesStartAtDefaultHighWhenUnlocking() {
        for (mode in listOf(4, 7, 8)) {
            assertEquals(SettingsRefreshRateSelectionRules.Initialization(120, 3), initialize(mode))
        }
    }

    @Test fun autoAndStandardRemainSelectedWithDefaultHighReadyForLater() {
        assertEquals(SettingsRefreshRateSelectionRules.Initialization(120, null), initialize(0))
        assertEquals(SettingsRefreshRateSelectionRules.Initialization(120, null), initialize(2))
        assertNull(initialize(0, 120))
        assertNull(initialize(2, 120))
    }

    @Test fun activeNative90ModeIsNotReplacedWith120() {
        assertNull(initialize(1, 90))
        assertEquals(SettingsRefreshRateSelectionRules.Initialization(90, null), initialize(1, 144))
    }

    @Test fun explicitChoicesAreNotResetByRepeatedEnableCalls() {
        for ((mode, choice) in listOf(1 to 90, 3 to 120, 8 to 144, 8 to 165, 8 to 185)) {
            assertNull(initialize(mode, choice, wasEnabled = true))
        }
    }

    @Test fun disablingDoesNotChangeRefreshRateOrRememberedChoice() {
        assertNull(initialize(8, 144, wasEnabled = true, enabled = false))
        assertNull(initialize(3, 144, wasEnabled = false, enabled = false))
    }

    @Test fun missingAndUnknownModesAreNotGuessed() {
        for (mode in listOf(-1, 5, 6, 9, 99)) assertNull(initialize(mode))
        assertEquals(SettingsRefreshRateSelectionRules.Initialization(120, null), initialize(3, -1))
    }

    @Test fun defaultHighGuardIsFeatureGatedAndDoesNotBlockExplicitExtremeModes() {
        assertTrue(SettingsRefreshRateSelectionRules.keepDefaultHighMode(true, 3))
        assertFalse(SettingsRefreshRateSelectionRules.keepDefaultHighMode(false, 3))
        for (mode in listOf(0, 1, 2, 4, 7, 8)) {
            assertFalse(SettingsRefreshRateSelectionRules.keepDefaultHighMode(true, mode))
        }
    }
}
