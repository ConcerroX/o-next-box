package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class DataSimSignalRulesTest {
    @Test fun followsEitherDataSimWithoutDependingOnSlotOrder() {
        assertEquals(listOf(7), DataSimSignalRules.select(listOf(7, 2), true, 7))
        assertEquals(listOf(2), DataSimSignalRules.select(listOf(7, 2), true, 2))
        assertEquals(listOf(7), DataSimSignalRules.select(listOf(2, 7), true, 7))
        assertEquals(listOf(2), DataSimSignalRules.select(listOf(7, 2, 9), true, 2))
    }
    @Test fun missingOrInvalidSelectionKeepsNativeSignals() {
        val source = listOf(7, 2)
        assertSame(source, DataSimSignalRules.select(source, false, 2))
        assertSame(source, DataSimSignalRules.select(source, true, -1))
        assertSame(source, DataSimSignalRules.select(source, true, 9))
        assertEquals(listOf(7), DataSimSignalRules.select(listOf(7), true, 2))
        assertEquals(emptyList<Int>(), DataSimSignalRules.select(emptyList(), true, 2))
    }
    @Test fun nativeInPlaceReverseCannotCorruptSourceOrNextDataSwitch() {
        val source = arrayListOf(7, 2)
        val nativeInput = ArrayList(DataSimSignalRules.select(source, true, 2))
        nativeInput.reverse()
        nativeInput.clear()
        assertEquals(listOf(7, 2), source)
        assertEquals(listOf(7), DataSimSignalRules.select(source, true, 7))
    }
}
