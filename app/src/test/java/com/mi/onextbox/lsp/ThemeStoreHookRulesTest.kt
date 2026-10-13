package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class ThemeStoreHookRulesTest {
    @Test fun expiryFilteringPreservesCallsAndUnrelatedBroadcasts() {
        for (action in listOf("THEME_TRIAL_EXPIRE", "FONT_TRIAL_EXPIRE", "LIVE_WP_TRIAL_EXPIRE",
            "SKU_SYSTEMUI_TRIAL_EXPIRE", "SKU_LOCKSCREEN_TRIAL_EXPIRE", "RESOURCE_EXPIRE", "VIP_EXPIRE")) {
            assertTrue(ThemeStoreHookRules.skipExpiry(true, "com.nearme.themespace.action.$action"))
            assertFalse(ThemeStoreHookRules.skipExpiry(false, "com.nearme.themespace.action.$action"))
        }
        assertFalse(ThemeStoreHookRules.skipExpiry(true, "com.nearme.themespace.action.CALL_ACTIVE"))
        assertFalse(ThemeStoreHookRules.skipExpiry(true, "android.intent.action.BOOT_COMPLETED"))
        assertFalse(ThemeStoreHookRules.skipExpiry(true, "unrelated.THEME_TRIAL_EXPIRE"))
        assertFalse(ThemeStoreHookRules.skipExpiry(true, null))
    }
    @Test fun replacementTypesMatchPrimitiveAndBoxedGetters() {
        assertEquals(1, ThemeStoreHookRules.numericValue(Int::class.javaPrimitiveType!!, 1))
        assertEquals(1, ThemeStoreHookRules.numericValue(Int::class.javaObjectType, 1))
        assertEquals(0.0, ThemeStoreHookRules.numericValue(Double::class.javaPrimitiveType!!, 0))
        assertEquals(0.0, ThemeStoreHookRules.numericValue(Double::class.javaObjectType, 0))
        assertEquals(999L, ThemeStoreHookRules.numericValue(Long::class.javaObjectType, 999))
        assertNull(ThemeStoreHookRules.numericValue(String::class.java, 1))
        assertNull(ThemeStoreHookRules.numericValue(Boolean::class.javaPrimitiveType!!, 1))
    }
}
