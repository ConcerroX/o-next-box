package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class VisualCleanupRulesTest {
    @Test fun floatingAppModesAreExcludedButSplitAndFullscreenRemain() {
        for (mode in listOf(2, 5, 100)) assertTrue(ScreenshotFloatingRules.exclude(mode, 1, "app.test"))
        for (mode in listOf(0, 1, 6, 120)) assertFalse(ScreenshotFloatingRules.exclude(mode, 1, "app.test"))
        assertFalse(ScreenshotFloatingRules.exclude(100, 2000, "com.android.systemui"))
        assertFalse(ScreenshotFloatingRules.exclude(100, 2011, "keyboard.test"))
    }
    @Test fun appOverlaysAreExcludedWithoutHidingSystemChrome() {
        assertTrue(ScreenshotFloatingRules.exclude(1, 2038, "app.test"))
        for (pkg in listOf(null, "", "android", "com.android.systemui", "com.oplus.screenshot"))
            assertFalse(ScreenshotFloatingRules.exclude(1, 2038, pkg))
    }
    @Test fun exclusionPreservesNativeLayersAndDoesNotHideWindows() {
        val source = File("src/main/java/com/mi/onextbox/lsp/HideScreenshotFloatingHooker.kt").readText()
        assertTrue(source.contains("original.filterIsInstance<SurfaceControl>().toMutableList()"))
        assertTrue(source.contains("layer.isValid"))
        assertFalse(source.contains(".release()"))
        assertFalse(source.contains(".setVisibility("))
    }
}
