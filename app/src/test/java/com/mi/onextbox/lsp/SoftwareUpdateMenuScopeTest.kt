package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class SoftwareUpdateMenuScopeTest {
    @Test fun nativeDeveloperSettingIsNotOverriddenOutsideMenuCreation() {
        val scope = SoftwareUpdateMenuScope()
        assertFalse(scope.overrides("development_settings_enabled"))
        scope.enter(false) { assertFalse(scope.overrides("development_settings_enabled")) }
    }

    @Test fun onlyTheLocalMenuDeveloperFlagIsOverridden() {
        val scope = SoftwareUpdateMenuScope()
        scope.enter(true) {
            assertTrue(scope.overrides("development_settings_enabled"))
            for (key in listOf(null, "", "auto_download_network_type", "can_update_at_night", "development_settings_enabled_other")) {
                assertFalse(scope.overrides(key))
            }
        }
        assertFalse(scope.overrides("development_settings_enabled"))
    }

    @Test fun nestedCallsAndExceptionsRestoreThePreviousScope() {
        val scope = SoftwareUpdateMenuScope()
        scope.enter(true) {
            scope.enter(false) { assertFalse(scope.overrides("development_settings_enabled")) }
            assertTrue(scope.overrides("development_settings_enabled"))
            assertThrows(IllegalStateException::class.java) { scope.enter(false) { error("native menu failed") } }
            assertTrue(scope.overrides("development_settings_enabled"))
        }
        assertFalse(scope.overrides("development_settings_enabled"))
    }

    @Test fun menuOverridesNeverLeakToOtherThreads() {
        val scope = SoftwareUpdateMenuScope()
        scope.enter(true) {
            var seen: Boolean? = null
            Thread { seen = scope.overrides("development_settings_enabled") }.also { it.start(); it.join() }
            assertEquals(false, seen)
        }
    }
}
