package com.mi.onextbox.refresh

import org.junit.Assert.*
import org.junit.Test

class RefreshRateModeResolverTest {
    private val saved = SavedRefreshRateMode(4, 4, 1440, 3168, 144.00002f)

    @Test fun staleBootIndexIsMatchedByResolutionAndRate() {
        val modes = listOf("id=4, config=4, 1080x2376@60.0", "id=17, config=2, 1440x3168@144.0")
        assertEquals(2, RefreshRateModeResolver.resolve(saved, modes)?.surfaceFlingerModeIndex)
    }
    @Test fun sameRateAtDifferentResolutionIsNotApplied() {
        assertNull(RefreshRateModeResolver.resolve(saved, listOf("id=4, config=4, 1080x2376@144.0")))
    }
    @Test fun duplicateRatesPreserveTheSavedNativeModeWhenPossible() {
        val mode = saved.copy(id = 3, surfaceFlingerModeIndex = 3, refreshRate = 120f)
        assertEquals(3, RefreshRateModeResolver.resolve(mode, listOf(
            "id=0, config=0, 1440x3168@120.00001", "id=3, config=3, 1440x3168@120.00001"))?.surfaceFlingerModeIndex)
    }
    @Test fun malformedAndUnavailableModesFailWithoutWritingAnArbitraryIndex() {
        assertNull(RefreshRateModeResolver.resolve(saved, listOf("Result: Parcel(NULL)", "id=4, config=-1, 1440x3168@144.0")))
        assertNull(RefreshRateModeResolver.resolve(saved.copy(refreshRate = Float.NaN), listOf("id=4, config=4, 1440x3168@144.0")))
    }
}
