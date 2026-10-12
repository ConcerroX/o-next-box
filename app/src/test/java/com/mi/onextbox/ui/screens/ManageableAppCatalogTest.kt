package com.mi.onextbox.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageableAppCatalogTest {
    @Test fun installedUserAppsDoNotRequireAnEnabledLauncher() {
        assertTrue(includeInManagementCatalog(true, false, false))
        assertTrue(includeInManagementCatalog(true, false, true))
    }

    @Test fun systemAppsWithLauncherAreIncluded() {
        assertTrue(includeInManagementCatalog(true, true, true))
        assertFalse(includeInManagementCatalog(true, true, false))
    }

    @Test fun uninstalledEntriesAreExcludedEvenWithLauncherOrRetainedData() {
        for (system in listOf(false, true)) {
            for (launcher in listOf(false, true)) {
                assertFalse(includeInManagementCatalog(false, system, launcher))
            }
        }
    }
}
