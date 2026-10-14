package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class LauncherGestureRulesTest {
    @Test fun splitSuppressionKeepsFloatingCapsuleAndDisabledBehaviorNative() {
        assertTrue(LauncherGestureRules.blockSplit(true, "SPLIT_WINDOW"))
        assertFalse(LauncherGestureRules.blockSplit(false, "SPLIT_WINDOW"))
        listOf(null, "NONE", "FLOATING_WINDOW", "CAPSULE", "OTHER").forEach { assertFalse(LauncherGestureRules.blockSplit(true, it)) }
    }
    @Test fun disabledSecondaryMenuClearsOnlyItsGroupingCount() {
        listOf(0, 1, 2, 5, Int.MAX_VALUE).forEach {
            assertEquals(0, LauncherGestureRules.secondaryMenuGroupingCount(true, it))
        }
    }
    @Test fun switchOffPreservesNativeGroupingCounts() {
        listOf(-1, 0, 1, 2, 5, Int.MAX_VALUE).forEach {
            assertEquals(it, LauncherGestureRules.secondaryMenuGroupingCount(false, it))
        }
    }
    @Test fun fullWidthAcceptsBothSidesOfTheNativeTriggerRow() {
        listOf(-500, 0, 500).forEach {
            assertEquals(53, LauncherGestureRules.selectAxis(49, 53, it, 1000, 0, 100))
            assertEquals(52, LauncherGestureRules.selectAxis(50, 52, it, 1000, 1, 100))
        }
    }
    @Test fun widthHasDefinedBoundsAndDoesNotActivateEarlyGestureStages() {
        assertEquals(50, LauncherGestureRules.width(1))
        assertEquals(100, LauncherGestureRules.width(500))
        listOf(-1, 0, 19, 35).forEach { assertEquals(it, LauncherGestureRules.selectAxis(it, 53, 0, 1000, 0, 100)) }
        assertEquals(49, LauncherGestureRules.selectAxis(49, -1, 0, 1000, 0, 100))
    }
    @Test fun narrowerAreasPreserveOtherOptionsOutsideTheFloatingRegion() {
        assertEquals(49, LauncherGestureRules.selectAxis(49, 53, -251, 1000, 0, 75))
        assertEquals(53, LauncherGestureRules.selectAxis(49, 53, -250, 1000, 0, 75))
        assertEquals(51, LauncherGestureRules.selectAxis(51, 53, -1, 1000, 0, 50))
    }
    @Test fun reverseLandscapeMirrorsTheNativeFloatingSide() {
        assertEquals(53, LauncherGestureRules.selectAxis(49, 53, 250, 1000, 3, 75))
        assertEquals(49, LauncherGestureRules.selectAxis(49, 53, 251, 1000, 3, 75))
    }
    @Test fun missingGeometryAndUnknownRotationKeepTheOriginalAxis() {
        assertEquals(49, LauncherGestureRules.selectAxis(49, 53, 0, 0, 0, 100))
        assertEquals(49, LauncherGestureRules.selectAxis(49, 53, 0, 1000, 4, 100))
    }
}
