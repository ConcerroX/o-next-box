package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class QsTileCornerOwnershipTest {
    private data class Provider(val radius: Int)

    @Test fun aSliderChangeKeepsTheRealNativeProviderForRestoration() {
        val native = Provider(32)
        val custom = Provider(8)
        val state = QsTileCornerOwnership(native, custom, 8f)
        assertSame(native, state.originalFor(custom))
        assertFalse(state.matches(custom, 12f))
        assertTrue(state.shouldRestore(custom))
    }
    @Test fun anExternalThemeUpdateBecomesTheNewNativeBaseline() {
        val state = QsTileCornerOwnership(Provider(32), Provider(8), 8f)
        val theme = Provider(16)
        assertSame(theme, state.originalFor(theme))
        assertFalse(state.shouldRestore(theme))
    }
    @Test fun equalButDifferentProvidersAreNotMistakenForModuleOwnership() {
        val custom = Provider(8)
        val state = QsTileCornerOwnership(Provider(32), custom, 8f)
        val external = Provider(8)
        assertEquals(custom, external)
        assertFalse(state.matches(external, 8f))
        assertFalse(state.shouldRestore(external))
        assertSame(external, state.originalFor(external))
    }
    @Test fun aNativeEqualityNoOpDoesNotCauseAnAllocationOnEveryFrame() {
        val native = Provider(24)
        val offered = Provider(24)
        val state = QsTileCornerOwnership(native, offered, 24f).accepted(native)
        assertTrue(state.matches(native, 24f))
        assertSame(native, state.originalFor(native))
        assertSame(native, state.original)
    }
    @Test fun acceptedCustomProviderMatchesUntilRadiusOrProviderChanges() {
        val custom = Provider(8)
        val state = QsTileCornerOwnership(Provider(32), custom, 8f).accepted(custom)
        assertTrue(state.matches(custom, 8f))
        assertFalse(state.matches(custom, 0f))
        assertFalse(state.matches(Provider(8), 8f))
    }
}
