package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class LauncherGridRulesTest {
    @Test fun missingAndMalformedConfigurationsStayDisabled() {
        for (value in listOf(null, "", "1", "1:4", "true:4:7", "1:4:7:0", "1:x:7", "1:4:-7",
            "1:2:7", "1:7:7", "1:4:11", "1:99999999999999999999:7")) {
            assertEquals(value, LauncherGridState(), LauncherGridRules.parse(value))
        }
    }

    @Test fun allSupportedCombinationsRoundTripAtomically() {
        for (columns in LauncherGridRules.MIN_COLUMNS..LauncherGridRules.MAX_COLUMNS) {
            for (rows in LauncherGridRules.MIN_ROWS..LauncherGridRules.MAX_ROWS) {
                for (enabled in listOf(false, true)) {
                    val state = LauncherGridState(enabled, LauncherGrid(columns, rows))
                    assertEquals(state, LauncherGridRules.parse(LauncherGridRules.encode(state)))
                }
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidGridCannotBePersisted() {
        LauncherGridRules.encode(LauncherGridState(true, LauncherGrid(0, 0)))
    }

    @Test fun expandsEveryCombinationWhilePreservingNativeOptionsAndWordlessPairs() {
        val stock = listOf(LauncherGridOption(4, 6, 7), LauncherGridOption(5, 7, 9))
        val result = LauncherGridRules.expand(stock, LauncherGrid(5, 8))
        assertEquals(8, result.size)
        assertEquals(stock, result.take(2))
        for (columns in 4..5) for (rows in 6..8) {
            assertTrue(result.contains(LauncherGridOption(columns, rows, rows + 1)))
        }
        assertEquals(LauncherGridOption(5, 8, 8), result.last())
    }

    @Test fun existingPairsAreNotDuplicatedAndExpansionIsIdempotent() {
        val stock = listOf(LauncherGridOption(4, 6, 7))
        assertEquals(stock + LauncherGridOption(4, 6, 6), LauncherGridRules.expand(stock, LauncherGrid(4, 6)))
        val expanded = LauncherGridRules.expand(stock, LauncherGrid(6, 10))
        assertSame(expanded, LauncherGridRules.expand(expanded, LauncherGrid(6, 10)))
        assertEquals(16, expanded.size)
    }

    @Test fun invalidCustomOptionKeepsTheOriginalList() {
        val stock = listOf(LauncherGridOption(4, 6, 7))
        assertSame(stock, LauncherGridRules.expand(stock, LauncherGrid(-1, 99)))
    }

    @Test fun smallerThanTheNativeBaselineRemainsSelectable() {
        assertEquals(listOf(LauncherGridOption(3, 4, 5)), LauncherGridRules.combinations(LauncherGrid(3, 4)))
        assertEquals(listOf(LauncherGridOption(4, 7, 8)),
            LauncherGridRules.combinations(LauncherGrid()).filter { it.textRows == 7 })
    }

    @Test fun validationAllowsOnlyGeneratedOptionsAndNativeSingleDimensionWildcards() {
        val maximum = LauncherGrid(5, 8)
        assertTrue(LauncherGridRules.supports(maximum, 4, 7, false))
        assertTrue(LauncherGridRules.supports(maximum, 5, 9, true))
        assertTrue(LauncherGridRules.supports(maximum, -1, 9, true))
        assertTrue(LauncherGridRules.supports(maximum, 5, -1, false))
        for ((columns, rows) in listOf(6 to 8, 4 to 5, 0 to 7, -2 to 7, -1 to -1, 4 to 9)) {
            assertFalse(LauncherGridRules.supports(maximum, columns, rows, false))
        }
        assertFalse(LauncherGridRules.supports(maximum, 4, 6, true))
    }

    @Test fun maximumWordlessRowFitsThePreviewWithoutBecomingAValidSetting() {
        assertTrue(LauncherGridRules.supports(LauncherGrid(6, 10), 6, 11, true))
        assertFalse(LauncherGridRules.valid(LauncherGrid(6, 11)))
        assertNotNull(LauncherGridRules.previewCellSize(90, 150, 8, 10, LauncherGrid(6, 11), 18, 18))
    }

    @Test fun directTargetKeepsTheExactRowCountWithOrWithoutIconLabels() {
        for (columns in LauncherGridRules.MIN_COLUMNS..LauncherGridRules.MAX_COLUMNS) {
            for (rows in LauncherGridRules.MIN_ROWS..LauncherGridRules.MAX_ROWS) {
                val grid = LauncherGrid(columns, rows)
                val options = LauncherGridRules.expand(emptyList(), grid)
                for (wordless in listOf(false, true)) {
                    assertTrue(options.any { it.grid(wordless) == grid })
                    assertTrue(LauncherGridRules.supports(grid, columns, rows, wordless))
                }
            }
        }
    }

    @Test fun densePreviewsKeepPositiveGapsInsideTheirBounds() {
        for (columns in LauncherGridRules.MIN_COLUMNS..LauncherGridRules.MAX_COLUMNS) {
            for (rows in LauncherGridRules.MIN_ROWS..LauncherGridRules.MAX_ROWS) {
                val size = LauncherGridRules.previewCellSize(90, 150, 8, 10, LauncherGrid(columns, rows), 18, 18)!!
                assertTrue(size >= 1)
                assertTrue(size <= 18)
                assertTrue(size * columns + columns - 1 <= 74)
                assertTrue(size * rows + rows - 1 <= 130)
            }
        }
    }

    @Test fun normalPreviewDoesNotEnlargeNativeCells() {
        assertEquals(12, LauncherGridRules.previewCellSize(200, 300, 8, 10, LauncherGrid(), 12, 16))
    }

    @Test fun unmeasuredOrInsufficientPreviewsDoNotDivideByZero() {
        assertNull(LauncherGridRules.previewCellSize(0, 0, 8, 10, LauncherGrid(), 12, 12))
        assertNull(LauncherGridRules.previewCellSize(5, 5, 8, 10, LauncherGrid(), 12, 12))
        assertNull(LauncherGridRules.previewCellSize(100, 100, 0, 0, LauncherGrid(1, 1), 12, 12))
    }
}
