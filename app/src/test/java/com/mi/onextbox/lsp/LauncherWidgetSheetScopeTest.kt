package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class LauncherWidgetSheetScopeTest {
    @Test fun ordinaryNativeActionsAndSheetsAreNotOwned() {
        val scope = LauncherWidgetSheetScope()
        assertFalse(scope.ownsAction(Any()))
        assertFalse(scope.ownsAction(null))
        assertFalse(scope.ownsSheet(Any()))
        assertFalse(scope.recordOpeningSheet(Any()))
        assertEquals(emptyList<Any>(), scope.openingSheets())
    }

    @Test fun onlyExplicitlyRestoredActionsAreOwned() {
        val scope = LauncherWidgetSheetScope()
        val action = Any()
        scope.recordAction(action)
        assertTrue(scope.ownsAction(action))
        assertFalse(scope.ownsAction(Any()))
    }

    @Test fun onlySheetsCreatedWhileOpeningTheRestoredActionAreMarked() {
        val scope = LauncherWidgetSheetScope()
        val sheet = Any()
        scope.duringAction {
            assertTrue(scope.recordOpeningSheet(sheet))
            assertTrue(scope.ownsSheet(sheet))
            assertEquals(listOf(sheet), scope.openingSheets())
        }
        assertTrue(scope.ownsSheet(sheet))
        assertEquals(emptyList<Any>(), scope.openingSheets())
    }

    @Test fun panelOwnershipPersistsForMeasureAndProviderRefreshAfterTheClickReturns() {
        val scope = LauncherWidgetSheetScope()
        val action = Any()
        val sheet = Any()
        scope.recordAction(action)
        scope.duringAction { scope.recordOpeningSheet(sheet) }
        repeat(5) { assertTrue(scope.ownsSheet(sheet)) }
        assertTrue(scope.ownsAction(action))
        assertFalse(scope.recordOpeningSheet(Any()))
    }

    @Test fun openingSnapshotsCannotMutateTheFramesUsedForFailureCleanup() {
        val scope = LauncherWidgetSheetScope()
        val first = Any()
        val second = Any()
        scope.duringAction {
            scope.recordOpeningSheet(first)
            val snapshot = scope.openingSheets()
            scope.recordOpeningSheet(second)
            assertEquals(listOf(first), snapshot)
            assertEquals(listOf(first, second), scope.openingSheets())
        }
    }

    @Test fun nestedClicksRestoreTheOuterCleanupFrame() {
        val scope = LauncherWidgetSheetScope()
        val outer = Any()
        val inner = Any()
        scope.duringAction {
            scope.recordOpeningSheet(outer)
            scope.duringAction {
                assertEquals(emptyList<Any>(), scope.openingSheets())
                scope.recordOpeningSheet(inner)
                assertEquals(listOf(inner), scope.openingSheets())
            }
            assertEquals(listOf(outer), scope.openingSheets())
        }
        assertTrue(scope.ownsSheet(outer))
        assertTrue(scope.ownsSheet(inner))
    }

    @Test fun failedClicksNeverMarkLaterUnrelatedPanels() {
        val scope = LauncherWidgetSheetScope()
        runCatching { scope.duringAction { scope.recordOpeningSheet(Any()); error("Native inflation failed") } }
        assertFalse(scope.recordOpeningSheet(Any()))
        assertEquals(emptyList<Any>(), scope.openingSheets())
    }

    @Test fun anotherThreadCannotInheritAnOpeningFrame() {
        val scope = LauncherWidgetSheetScope()
        val otherSheet = Any()
        scope.duringAction {
            var marked = true
            Thread { marked = scope.recordOpeningSheet(otherSheet) }.apply { start(); join() }
            assertFalse(marked)
        }
        assertFalse(scope.ownsSheet(otherSheet))
    }
}
