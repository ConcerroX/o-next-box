package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class QsTileCornerRulesTest {
    @Test fun savedRadiusIsBounded() {
        assertEquals(0, QsTileCornerRules.normalize(Int.MIN_VALUE))
        assertEquals(0, QsTileCornerRules.normalize(0))
        assertEquals(24, QsTileCornerRules.normalize(24))
        assertEquals(64, QsTileCornerRules.normalize(Int.MAX_VALUE))
    }
    @Test fun dpConversionUsesTheTileDisplayDensity() {
        assertEquals(72f, QsTileCornerRules.pixels(24, 3f)!!, 0f)
        assertEquals(30f, QsTileCornerRules.pixels(24, 1.25f)!!, 0f)
    }
    @Test fun radiusNeverExceedsHalfOfTheActualBackgroundShortSide() {
        assertEquals(50f, QsTileCornerRules.pixels(64, 3f, 300, 100)!!, 0f)
        assertEquals(50f, QsTileCornerRules.pixels(64, 3f, 100, 300)!!, 0f)
        assertEquals(51f, QsTileCornerRules.pixels(64, 3f, 102, 102)!!, 0f)
    }
    @Test fun zeroProducesSquareCornersEvenOnCircularTiles() {
        assertEquals(0f, QsTileCornerRules.pixels(0, 3f, 100, 100)!!, 0f)
    }
    @Test fun invalidDensityRetainsNativeGeometry() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { assertNull(QsTileCornerRules.pixels(24, it)) }
        assertNull(QsTileCornerRules.pixels(64, Float.MAX_VALUE))
    }
    @Test fun notYetLaidOutTilesUseDpUntilRealBoundsAreKnown() {
        assertEquals(72f, QsTileCornerRules.pixels(24, 3f, 0, 100)!!, 0f)
        assertEquals(72f, QsTileCornerRules.pixels(24, 3f, 100, 0)!!, 0f)
        assertEquals(20f, QsTileCornerRules.pixels(24, 3f, 100, 40)!!, 0f)
    }
}
