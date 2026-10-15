package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class DeveloperActivationEntryContractTest {
    private val source = File("src/main/java/com/mi/onextbox/ui/settings/DeveloperActivationEntry.kt").readText()

    @Test fun confirmationResetsOnlyOnboardingAndDiscardsSavedCompletedUi() {
        assertTrue(source.contains("CouiConfirmDialog("))
        assertTrue(source.indexOf("OnboardingPreferences.reset(context)") > source.indexOf("onPositive ="))
        assertTrue(source.contains("check(withContext(Dispatchers.IO)"))
        assertTrue(source.contains("Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK"))
        assertTrue(source.contains("UpdateNotificationScheduler.cancel(context)"))
        assertFalse(source.contains("ConfigBackup.resetToDefaults"))
        assertFalse(source.contains("LspPreferenceStore"))
    }

    @Test fun entryIsInsideDeveloperPageAndAvailableToBothUiStyles() {
        val settings = File("src/main/java/com/mi/onextbox/ui/settings/SettingsMainPage.kt").readText()
            .substringAfter("fun DeveloperOptionsPage(")
        assertTrue(settings.contains("DeveloperActivationEntry()"))
        assertFalse(source.contains("AppUiStyle"))
        assertTrue(source.contains("if (!opening)"))
    }
}
