package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class LargeNetworkTypeHookerTest {
    @Test fun boldRequiresBothParentSwitches() {
        assertEquals(700, LargeNetworkTypeHooker.fontWeight(true, true, true, 400))
        assertEquals(800, LargeNetworkTypeHooker.fontWeight(true, true, true, 800))
        assertEquals(400, LargeNetworkTypeHooker.fontWeight(false, true, true, 400))
        assertEquals(400, LargeNetworkTypeHooker.fontWeight(true, false, true, 400))
        assertEquals(400, LargeNetworkTypeHooker.fontWeight(true, true, false, 400))
    }
    @Test fun followsFontOnlyForEnabledLargeNetworkText() {
        assertTrue(LargeNetworkTypeHooker.shouldFollowFont(true, true, 25))
        assertFalse(LargeNetworkTypeHooker.shouldFollowFont(false, true, 25))
        assertFalse(LargeNetworkTypeHooker.shouldFollowFont(true, false, 25))
        assertFalse(LargeNetworkTypeHooker.shouldFollowFont(true, true, 70))
    }
    @Test fun onlyEnablesExactLayoutFeature() {
        assertTrue(LargeNetworkTypeHooker.shouldEnable(true, LargeNetworkTypeHooker.FEATURE))
        assertFalse(LargeNetworkTypeHooker.shouldEnable(false, LargeNetworkTypeHooker.FEATURE))
        assertFalse(LargeNetworkTypeHooker.shouldEnable(true, null))
        assertFalse(LargeNetworkTypeHooker.shouldEnable(true, "com.android.systemui.statusbar_show_big_5guwc"))
        assertFalse(LargeNetworkTypeHooker.shouldEnable(true, "com.android.systemui.signal_layout_att"))
    }
}
