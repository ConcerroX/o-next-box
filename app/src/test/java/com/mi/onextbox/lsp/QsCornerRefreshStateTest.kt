package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class QsCornerRefreshStateTest {
    private val state = QsCornerRefreshState(24, 200, 300, 3f, 1, 2, true, 0)

    @Test fun unchangedFramesDoNotScheduleAnotherNativeUpdate() {
        val gate = QsCornerRefreshGate()
        assertTrue(gate.changed(state))
        gate.applied(state)
        repeat(120) { assertFalse(gate.changed(state.copy())) }
    }

    @Test fun disableResizeThemeReparentAndReattachAreNotLost() {
        for (next in listOf(state.copy(radiusDp = null), state.copy(radiusDp = 0),
            state.copy(width = 201), state.copy(height = 301), state.copy(density = 4f),
            state.copy(backgroundId = 4), state.copy(parentId = 5),
            state.copy(shown = false), state.copy(direction = 1))) {
            val gate = QsCornerRefreshGate()
            gate.applied(state)
            assertTrue(next.toString(), gate.changed(next))
            gate.applied(next)
            assertFalse(gate.changed(next))
        }
        val gate = QsCornerRefreshGate()
        gate.applied(state)
        gate.invalidate()
        assertTrue(gate.changed(state))
    }

    @Test fun notReadyOrFailedUpdateNeverAcknowledgesTheSnapshot() {
        val gate = QsCornerRefreshGate()
        repeat(8) { assertTrue(gate.changed(state)) }
        gate.applied(state)
        assertFalse(gate.changed(state))
        val rebuilt = state.copy(content = listOf(42, 200, 300, 12))
        repeat(8) { assertTrue(gate.changed(rebuilt)) }
        gate.applied(rebuilt)
        assertFalse(gate.changed(rebuilt))
    }

    @Test fun internalMaterialAndChildChangesAreDetectedWithAnUnchangedOuterView() {
        val gate = QsCornerRefreshGate()
        val before = state.copy(content = listOf(10, 200, 300, 20, false))
        for (after in listOf(listOf(11, 200, 300, 20, false), listOf(10, 201, 300, 20, false),
            listOf(10, 200, 301, 20, false), listOf(10, 200, 300, 21, false), listOf(10, 200, 300, 20, true))) {
            gate.applied(before)
            assertTrue(gate.changed(before.copy(content = after)))
        }
    }

    @Test fun committingThePostApplyStateAvoidsASecondUpdateForOurOwnProvider() {
        val gate = QsCornerRefreshGate()
        val native = state.copy(content = listOf(10))
        val custom = state.copy(content = listOf(11))
        assertTrue(gate.changed(native))
        gate.applied(custom)
        repeat(120) { assertFalse(gate.changed(custom)) }
        assertTrue(gate.changed(native))
    }

    @Test fun unchangedRadiusDoesNotHideMaterialRecreationOrAnimation() {
        val material = QsSliderMaterialState(24f, 3f, 1f, 0, 0, 100, 300, 1, 2, true, false)
        assertEquals(material, material.copy())
        for (next in listOf(material.copy(radius = 25f), material.copy(weight = 2f),
            material.copy(scale = .9f), material.copy(left = 1), material.copy(top = 1),
            material.copy(right = 101), material.copy(bottom = 301),
            material.copy(baseId = 3), material.copy(activeId = 4),
            material.copy(stroke = false), material.copy(detail = true))) assertNotEquals(material, next)
    }

    @Test fun nativeSmoothnessIncludingNullSurvivesProviderOwnership() {
        val native = Any()
        val custom = Any()
        val owned = QsTileCornerOwnership(native, custom, 24f, 3f)
        assertTrue(owned.matches(custom, 24f, 3f))
        assertFalse(owned.matches(custom, 24f, 1f))
        assertFalse(owned.matches(custom, 24f, null))
        assertSame(native, owned.originalFor(custom))
        assertEquals(3f, owned.accepted(custom).weight)
        assertTrue(QsTileCornerOwnership(native, custom, 24f, null).matches(custom, 24f, null))
    }
}
