package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class LauncherNotificationDotCompatibilityTest {
    @Test fun hiddenAndInvalidAnimationFramesDoNotDrawShadows() {
        for (scale in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFalse(LauncherDotShadowRules.shouldDraw(10, scale))
        }
        assertFalse(LauncherDotShadowRules.shouldDraw(0, 1f))
        assertTrue(LauncherDotShadowRules.shouldDraw(10, 0.5f))
        assertTrue(LauncherDotShadowRules.shouldDraw(10, 1f))
    }

    @Test fun tintRestoresOriginalStateInsteadOfClearingOtherFilters() {
        val source = File("src/main/java/com/mi/onextbox/lsp/LauncherNotificationDotColorHooker.kt").readText()
        assertFalse(source.contains("clearColorFilter()"))
        assertTrue(source.contains("val original = drawable.colorFilter"))
        assertTrue(source.contains("drawable.colorFilter = original"))
        assertTrue(source.contains("restores.remove(param)?.asReversed()"))
        assertTrue(source.contains("\"mBadgeNumTextColor\", original"))
        assertTrue(source.contains("if (software !== bitmap) software.recycle()"))
    }
}
