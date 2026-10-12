package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class ScreenCaptureRulesTest {
    @Test fun allowsSecureFlagOnlyWhenOptedIn() {
        assertTrue(ScreenCaptureRules.canOverride(true, 0x2000, true, false))
        assertTrue(ScreenCaptureRules.canOverride(true, 0x2008, true, false))
        assertFalse(ScreenCaptureRules.canOverride(false, 0x2000, true, false))
        assertFalse(ScreenCaptureRules.canOverride(true, 0x8, true, false))
    }
    @Test fun retainsIndependentProtection() {
        assertFalse(ScreenCaptureRules.canOverride(true, 0x2000, false, false))
        assertFalse(ScreenCaptureRules.canOverride(true, 0x2000, true, true))
        assertFalse(ScreenCaptureRules.canOverride(true, 0x2000, false, true))
    }
}
