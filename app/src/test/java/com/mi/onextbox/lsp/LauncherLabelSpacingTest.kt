package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LauncherLabelSpacingTest {
    @Test fun settingClampsUntrustedOffsets() {
        assertEquals(0, LauncherGestureRules.iconLabelSpacing(-1))
        assertEquals(20, LauncherGestureRules.iconLabelSpacing(Int.MAX_VALUE))
        assertEquals(8, LauncherGestureRules.iconLabelSpacing(8))
    }

    @Test fun fixedHeightLabelsKeepTheirTextVisible() {
        assertEquals(8, LauncherLabelSpacingRules.labelTop(2, 6, 40, 0, 20))
        assertEquals(10, LauncherLabelSpacingRules.labelTop(2, 60, 30, 0, 20))
        assertEquals(2, LauncherLabelSpacingRules.labelTop(2, 60, 20, 0, 20))
        assertEquals(2, LauncherLabelSpacingRules.labelTop(2, 60, -2, 0, 20))
        assertEquals(2, LauncherLabelSpacingRules.labelTop(2, 0, 40, 0, 20))
    }

    @Test fun spacingDoesNotDependOnWorkspaceLoadOrderOrFailedLayout() {
        val source = File("src/main/java/com/mi/onextbox/lsp/LauncherIconLabelSpacingHooker.kt").readText()
        assertFalse(source.contains("workspaceBasePaddingPx"))
        assertTrue(source.contains("param.result != true"))
        assertTrue(source.contains("label.layoutParams == null"))
        assertTrue(source.contains("it.name == \"updateIconBounds\""))
    }
}
