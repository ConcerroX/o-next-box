package com.mi.onextbox.lsp

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardDismissButtonRulesTest {
    @Test fun onlyXiaobuGestureDismissButtonIsHiddenWithoutRemovingItsSpace() {
        assertEquals(4, visibility(true, "com.oplus.keyboard", true, 0))
        assertEquals(0, visibility(false, "com.oplus.keyboard", true, 0))
        assertEquals(0, visibility(true, "com.google.android.inputmethod.latin", true, 0))
        assertEquals(0, visibility(true, "com.oplus.keyboard", false, 0))
    }

    @Test fun existingSystemHiddenStatesArePreserved() {
        for (stock in listOf(4, 8)) for (enabled in listOf(true, false)) {
            assertEquals(stock, visibility(enabled, "com.oplus.keyboard", true, stock))
        }
    }

    @Test fun onlyOurInvisibleOverrideReusesSavedVisibility() {
        assertEquals(0, KeyboardDismissButtonRules.stockVisibility(4, 0, false))
        assertEquals(4, KeyboardDismissButtonRules.stockVisibility(4, null, false))
        assertEquals(4, KeyboardDismissButtonRules.stockVisibility(4, 0, true))
        assertEquals(8, KeyboardDismissButtonRules.stockVisibility(8, 0, false))
        assertEquals(0, KeyboardDismissButtonRules.stockVisibility(0, 0, false))
    }

    private fun visibility(enabled: Boolean, pkg: String, gestural: Boolean, stock: Int) =
        KeyboardDismissButtonRules.visibility(enabled, pkg, gestural, stock)
}
