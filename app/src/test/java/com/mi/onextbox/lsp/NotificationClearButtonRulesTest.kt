package com.mi.onextbox.lsp

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationClearButtonRulesTest {
    @Test fun enabledButtonUsesTheNativeHeightAsItsDiameter() {
        assertEquals(44, NotificationClearButtonRules.width(true, 96, 44))
        assertEquals(132, NotificationClearButtonRules.width(true, 288, 132))
        assertEquals(60, NotificationClearButtonRules.width(true, 160, 60))
    }

    @Test fun disabledOrUnresolvedDimensionsKeepTheStockWidth() {
        assertEquals(96, NotificationClearButtonRules.width(false, 96, 44))
        for (height in listOf(-2, -1, 0)) {
            assertEquals(96, NotificationClearButtonRules.width(true, 96, height))
        }
        for (width in listOf(-2, -1, 0)) {
            assertEquals(width, NotificationClearButtonRules.width(true, width, 44))
        }
    }

    @Test fun alreadyCircularStockButtonsStayUnchanged() {
        assertEquals(44, NotificationClearButtonRules.width(true, 44, 44))
        assertEquals(44, NotificationClearButtonRules.width(false, 44, 44))
    }

    @Test fun restoreOnlyTouchesOurOverride() {
        assertEquals(96, NotificationClearButtonRules.restoreWidth(44, 96, 44))
        assertEquals(120, NotificationClearButtonRules.restoreWidth(120, 96, 44))
        assertEquals(96, NotificationClearButtonRules.restoreWidth(96, 96, 44))
        assertEquals(44, NotificationClearButtonRules.restoreWidth(44, -1, 44))
    }
}
