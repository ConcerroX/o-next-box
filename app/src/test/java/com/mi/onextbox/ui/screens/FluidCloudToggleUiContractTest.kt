package com.mi.onextbox.ui.screens

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FluidCloudToggleUiContractTest {
    private val source = listOf(
        File("src/main/java/com/mi/onextbox/ui/screens/FluidCloudFeaturesPage.kt"),
        File("app/src/main/java/com/mi/onextbox/ui/screens/FluidCloudFeaturesPage.kt"),
    ).first { it.isFile }.readText()

    @Test fun savingDoesNotTemporarilyDimTheToggleLabels() {
        listOf("applying", "mediaApplying", "browserApplying").forEach { state ->
            assertFalse(state, source.contains("enabled = !$state"))
        }
    }

    @Test fun pendingWritesStillRejectRepeatedToggleActions() {
        assertTrue(source.contains("if (applying) return@SettingsToggleRow"))
        assertTrue(source.contains("if (mediaApplying) return@SettingsToggleRow"))
        val browserAction = source.substringAfter("checked = browserEnabled,")
        assertTrue(browserAction.contains("if (!browserApplying)"))
    }

    @Test fun togglesUpdateImmediatelyAndReconcileWithSavedValues() {
        listOf("enabled", "mediaEnabled", "browserEnabled").forEach { state ->
            assertTrue(state, source.contains("$state = checked"))
        }
        assertTrue(source.contains("finally {\n                        enabled = LspConfig.isFluidCloudMaterialEnabled(context)"))
        assertTrue(source.contains("finally {\n                        mediaEnabled = FluidCloudMediaSettings.isEnabled(context)"))
        assertTrue(source.contains("browserEnabled = FluidCloudBrowserSettings.isEnabled(appContext)"))
    }

    @Test fun lifecycleAndPreferenceRefreshCannotOverwritePendingMediaChanges() {
        assertTrue(source.contains("if (!mediaApplying) mediaEnabled = FluidCloudMediaSettings.isEnabled(context)"))
        assertTrue(source.contains("if (!mediaApplying && (key == null || key == FluidCloudMediaSettings.ENABLED))"))
    }
}
