package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class QsCornerProbeScheduleTest {
    private fun QsCornerProbeSchedule.probe(time: Long, shown: Boolean = true, dp: Int? = 24, width: Int = 200) =
        shouldProbe(time, shown, dp, width, 300, 1, 2, 3f, 0)

    @Test fun stable120HzFramesOnlyRunTwoFallbackProbesPerSecond() {
        val schedule = QsCornerProbeSchedule()
        assertEquals(2, (0 until 120).count { schedule.probe(it * 1000L / 120) })
    }
    @Test fun nativeEventsAndLayoutAndPreferenceChangesDoNotWaitForFallback() {
        val schedule = QsCornerProbeSchedule()
        assertTrue(schedule.probe(0))
        assertFalse(schedule.probe(8))
        schedule.invalidate()
        assertTrue(schedule.probe(9))
        assertTrue(schedule.probe(10, width = 201))
        assertTrue(schedule.probe(11, dp = 0))
        assertTrue(schedule.probe(12, dp = null))
        assertFalse(schedule.probe(1000, dp = null))
    }
    @Test fun hiddenViewsDoNotProbeAndReopeningAlwaysRechecks() {
        val schedule = QsCornerProbeSchedule()
        repeat(120) { assertFalse(schedule.probe(it * 8L, shown = false)) }
        assertTrue(schedule.probe(1000))
        assertFalse(schedule.probe(1001, shown = false))
        assertTrue(schedule.probe(1002))
    }
    @Test fun animationGeometryDoesNotRebuildMaterialButThemeAndNewDrawablesDo() {
        val state = QsSliderMaterialState(24f, 3f, 1f, 0, 0, 100, 300, 1, 2, true, false)
        assertTrue(state.sameMaterial(state.copy(radius = 12f, left = 10, right = 200, bottom = 100)))
        for (next in listOf(state.copy(weight = 2f), state.copy(scale = .9f), state.copy(baseId = 3),
            state.copy(activeId = 4), state.copy(stroke = false), state.copy(detail = true), state.copy(right = 0)))
            assertFalse(state.sameMaterial(next))
    }
    class Receiver {
        private var radius = 1f
        fun radius() = radius
        fun setRadius(value: Float) { radius = value }
    }
    @Test fun cachedGettersAndFloatSettersReturnCurrentValues() {
        val first = Receiver()
        val second = Receiver()
        repeat(10) {
            QsCornerReflect.callMethod(first, "setRadius", it.toFloat())
            assertEquals(it.toFloat(), QsCornerReflect.callMethod(first, "radius"))
            assertEquals(1f, QsCornerReflect.callMethod(second, "radius"))
        }
    }
}
