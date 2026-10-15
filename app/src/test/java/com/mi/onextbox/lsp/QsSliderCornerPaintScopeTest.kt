package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class QsSliderCornerPaintScopeTest {
    private val progress = QsSliderCornerPaintScope.PROGRESS
    private val background = QsSliderCornerPaintScope.BACKGROUND
    @Test fun paintFieldsAreRestoredBeforeNativeGeometryOrInputRunsAgain() {
        val fields = mutableMapOf(progress to 31f, background to 31f, "mPaddingVertical" to 31f, "mProgress" to 100f)
        val scope = QsSliderCornerPaintScope({ fields.getValue(it) }, { key, value -> fields[key] = value })
        scope.paint(0f, 0f) {
            assertEquals(0f, fields.getValue(progress), 0f)
            assertEquals(0f, fields.getValue(background), 0f)
            assertEquals(31f, fields.getValue("mPaddingVertical"), 0f)
            assertEquals(100f, fields.getValue("mProgress"), 0f)
        }
        assertEquals(31f, fields.getValue(progress), 0f)
        assertEquals(31f, fields.getValue(background), 0f)
    }
    @Test fun nativeDrawingFailureCannotLeaveCustomValuesInGeometryState() {
        val fields = mutableMapOf(progress to 31f, background to 32f)
        val scope = QsSliderCornerPaintScope({ fields.getValue(it) }, { key, value -> fields[key] = value })
        runCatching { scope.paint(8f, 8f) { error("draw failure") } }
        assertEquals(31f, fields.getValue(progress), 0f)
        assertEquals(32f, fields.getValue(background), 0f)
    }
    @Test fun partialPreparationFailureRestoresBothFields() {
        val fields = mutableMapOf(progress to 31f, background to 32f)
        val scope = QsSliderCornerPaintScope({ fields.getValue(it) }, { key, value ->
            if (key == background && value == 8f) error("write failure")
            fields[key] = value
        })
        runCatching { scope.paint(8f, 8f) { fail("must not draw") } }
        assertEquals(31f, fields.getValue(progress), 0f)
        assertEquals(32f, fields.getValue(background), 0f)
    }
    @Test fun nestedDrawingRestoresThePreviousScope() {
        val fields = mutableMapOf(progress to 31f, background to 32f)
        val scope = QsSliderCornerPaintScope({ fields.getValue(it) }, { key, value -> fields[key] = value })
        scope.paint(8f, 8f) {
            scope.paint(4f, 4f) { assertEquals(4f, fields.getValue(progress), 0f) }
            assertEquals(8f, fields.getValue(progress), 0f)
        }
        assertEquals(31f, fields.getValue(progress), 0f)
    }
    @Test fun pressedCornerScalingIsPreservedWithoutTouchingTheNativeAnimation() {
        assertEquals(12f, QsSliderCornerPaintScope.scaled(8f, 48f, 32f), 0f)
        assertEquals(0f, QsSliderCornerPaintScope.scaled(0f, 48f, 32f), 0f)
        assertEquals(8f, QsSliderCornerPaintScope.scaled(8f, 0f, 0f), 0f)
        assertEquals(8f, QsSliderCornerPaintScope.scaled(8f, Float.NaN, 32f), 0f)
    }
}
