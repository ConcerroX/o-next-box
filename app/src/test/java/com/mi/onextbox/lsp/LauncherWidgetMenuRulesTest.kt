package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class LauncherWidgetMenuRulesTest {
    @Test fun normalAppIconsAreEligible() {
        assertTrue(LauncherWidgetMenuRules.eligible(0, false, false, false, false))
    }

    @Test fun foldersWidgetsPinnedShortcutsAndOtherNonAppItemsAreNotEligible() {
        for (type in listOf(-1, 1, 2, 4, 5, 6, 7, 8, 99, 100, 101, 102, 105, 202, Int.MAX_VALUE)) {
            assertFalse("type=$type", LauncherWidgetMenuRules.eligible(type, false, false, false, false))
        }
    }

    @Test fun disabledPromiseArchivedAndNonInstalledAppsAreOmitted() {
        for (flags in 1..15) {
            assertFalse(LauncherWidgetMenuRules.eligible(0, flags and 1 != 0, flags and 2 != 0,
                flags and 4 != 0, flags and 8 != 0))
        }
    }

    @Test fun appEligibilityIsIndependentOfSecondaryMenuGrouping() {
        for (disableSecondary in listOf(false, true)) {
            assertEquals(if (disableSecondary) 0 else 3, LauncherGestureRules.secondaryMenuGroupingCount(disableSecondary, 3))
            assertTrue(LauncherWidgetMenuRules.eligible(0, false, false, false, false))
        }
    }

    @Test fun missingNativeFactoryIsAppendedWithoutChangingTheSourceOrItsOrder() {
        val first = Any()
        val second = Any()
        val widgets = Any()
        val source = arrayListOf(first, second)
        val result = LauncherWidgetMenuRules.appendFactory(source, widgets)
        assertEquals(listOf(first, second, widgets), result)
        assertEquals(listOf(first, second), source)
    }

    @Test fun retainedNativeFactoryIsNotDuplicatedOrMoved() {
        val widgets = Any()
        val source = listOf(Any(), widgets, Any())
        assertSame(source, LauncherWidgetMenuRules.appendFactory(source, widgets))
        val restored = LauncherWidgetMenuRules.appendFactory(emptyList(), widgets)
        assertEquals(listOf(widgets), restored)
        assertSame(restored, LauncherWidgetMenuRules.appendFactory(restored, widgets))
    }

    @Test fun unrelatedDuplicatesAndEqualButDistinctFactoriesRemainIntact() {
        data class Factory(val name: String)
        val widgets = Factory("widgets")
        val other = Factory("widgets")
        val source = listOf(other, other)
        val result = LauncherWidgetMenuRules.appendFactory(source, widgets)
        assertEquals(3, result.size)
        assertSame(other, result[0])
        assertSame(other, result[1])
        assertSame(widgets, result[2])
    }
}
