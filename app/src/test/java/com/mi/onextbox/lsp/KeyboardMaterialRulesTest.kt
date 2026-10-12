package com.mi.onextbox.lsp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardMaterialRulesTest {
    @Test fun stockListIsIdentifiedByContentsRatherThanAnObfuscatedFieldName() {
        assertTrue(KeyboardMaterialRules.isMaterialAllowlist(KeyboardMaterialRules.stockPackages.toList()))
        assertTrue(KeyboardMaterialRules.isMaterialAllowlist(KeyboardMaterialRules.stockPackages + "com.example.extra"))
        assertFalse(KeyboardMaterialRules.isMaterialAllowlist(KeyboardMaterialRules.stockPackages - "com.android.settings"))
        assertFalse(KeyboardMaterialRules.isMaterialAllowlist(listOf("com.example.app")))
        assertFalse(KeyboardMaterialRules.isMaterialAllowlist(KeyboardMaterialRules.stockPackages + listOf(null)))
    }

    @Test fun enabledMaterialListAllowsThirdPartyApps() {
        assertTrue(KeyboardMaterialRules.shouldAllow(true, true, "com.mi.onextbox"))
        assertTrue(KeyboardMaterialRules.shouldAllow(true, true, "com.google.android.googlequicksearchbox"))
        assertTrue(KeyboardMaterialRules.shouldAllow(true, true, "com.android.settings"))
    }

    @Test fun disablingOrUsingAnotherListNeverOverridesMembership() {
        assertFalse(KeyboardMaterialRules.shouldAllow(false, true, "com.mi.onextbox"))
        assertFalse(KeyboardMaterialRules.shouldAllow(true, false, "com.mi.onextbox"))
    }

    @Test fun invalidAndNonPackageCandidatesKeepTheirStockResult() {
        listOf(null, 42, Any(), "", "android", " com.example.app", "com.example.app ",
            "com.example..app", "com.example/app", "a." + "b".repeat(256)).forEach {
            assertFalse(KeyboardMaterialRules.shouldAllow(true, true, it))
        }
    }
}
