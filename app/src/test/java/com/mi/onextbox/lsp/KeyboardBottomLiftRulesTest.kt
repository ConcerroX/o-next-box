package com.mi.onextbox.lsp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardBottomLiftRulesTest {
    @Test fun enabledRaisedKeyboardRefreshesOnlyWhenReady() {
        assertTrue(KeyboardBottomLiftRules.shouldRefresh(true, true, true, true, true, true))
    }

    @Test fun everyBoundaryPreservesStockBehavior() {
        repeat(6) { disabled ->
            val flags = BooleanArray(6) { it != disabled }
            assertFalse(KeyboardBottomLiftRules.shouldRefresh(flags[0], flags[1], flags[2],
                flags[3], flags[4], flags[5]))
        }
    }

    @Test fun dexSelectionRequiresBothNativeHeightAnchors() {
        val attached = "registerKbPositionRaiseObserver failed"
        val changed = "kbPositionRaiseObserver onChange:"
        assertTrue(KeyboardBottomLiftRules.containsHeightAnchors("dex\u0000$attached\u0000$changed".toByteArray()))
        assertFalse(KeyboardBottomLiftRules.containsHeightAnchors(attached.toByteArray()))
        assertFalse(KeyboardBottomLiftRules.containsHeightAnchors(changed.toByteArray()))
        assertFalse(KeyboardBottomLiftRules.containsHeightAnchors(byteArrayOf()))
    }
}
