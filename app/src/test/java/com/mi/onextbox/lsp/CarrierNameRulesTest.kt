package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class CarrierNameRulesTest {
    @Test fun emptyAndWhitespaceNamesMeanNativeText() {
        for (name in listOf(null, "", "  ", "\n\t\r", "\u202e\u2068")) {
            assertEquals("", CarrierNameRules.normalize(name))
            assertNull(CarrierNameRules.replacement(true, name, 0, 5, true, false))
        }
    }

    @Test fun namesRemainSingleLineWithoutControlOrBidiInjection() {
        assertEquals("China Mobile", CarrierNameRules.normalize(" China\nMobile "))
        assertEquals("China Mobile", CarrierNameRules.normalize("China\u2028Mobile"))
        assertEquals("Carrier", CarrierNameRules.normalize("\u202eCar\u0000rier\u2069"))
        assertEquals("SIM  1", CarrierNameRules.normalize("SIM  1"))
    }

    @Test fun nonLatinNamesAndEmojiAreSupported() {
        assertEquals("中国移动 📶", CarrierNameRules.normalize("中国移动 📶"))
        assertEquals("شركة اتصالات", CarrierNameRules.normalize("شركة اتصالات"))
        assertEquals("👨‍👩‍👧‍👦", CarrierNameRules.normalize("👨‍👩‍👧‍👦"))
    }

    @Test fun lengthLimitNeverSplitsASurrogatePair() {
        val value = "📶".repeat(40)
        val normalized = CarrierNameRules.normalize(value)
        assertEquals("📶".repeat(32), normalized)
        assertEquals(32, normalized.codePointCount(0, normalized.length))
        assertEquals("a".repeat(31) + "📶", CarrierNameRules.normalize("a".repeat(31) + "📶tail"))
    }

    @Test fun invalidLoneSurrogatesAreRemoved() {
        assertEquals("Carrier", CarrierNameRules.normalize("\ud800Carrier\udfff"))
    }

    @Test fun normalizationIsIdempotent() {
        for (value in listOf("Long name".repeat(20), "中国移动", "\u202e\nName 📶\t")) {
            assertEquals(CarrierNameRules.normalize(value), CarrierNameRules.normalize(CarrierNameRules.normalize(value)))
        }
    }

    @Test fun onlyReadyInServiceSimUsesItsAlias() {
        assertEquals("Custom", CarrierNameRules.replacement(true, "Custom", 0, 5, true, false))
        assertEquals("Custom", CarrierNameRules.replacement(true, "Custom", 1, 5, true, false))
        for (state in 0..10) if (state != 5) {
            assertNull(CarrierNameRules.replacement(true, "Custom", 0, state, true, false))
        }
        assertNull(CarrierNameRules.replacement(true, "Custom", 0, 5, false, false))
    }

    @Test fun disabledAirplaneAndUnknownSlotsKeepNativeText() {
        assertNull(CarrierNameRules.replacement(false, "Custom", 0, 5, true, false))
        assertNull(CarrierNameRules.replacement(true, "Custom", 0, 5, true, true))
        for (slot in listOf(-1, 2, 3, Int.MAX_VALUE)) {
            assertNull(CarrierNameRules.replacement(true, "Custom", slot, 5, true, false))
        }
    }

    @Test fun aliasesUsePhysicalSlotsNotCurrentDataSimOrSubscriptionOrder() {
        val names = CarrierNameSettings.State(true, "First", "Second")
        assertEquals("Second", names.name(1))
        assertEquals("First", names.name(0))
        assertEquals("", names.name(2))
        assertEquals("Second", names.copy(enabled = false).name(1))
    }

    @Test fun freshInstallIsOffAndUsesOriginalNames() {
        val defaults = CarrierNameSettings.State()
        assertFalse(defaults.enabled)
        assertEquals("", defaults.sim1)
        assertEquals("", defaults.sim2)
    }

    @Test fun secondarySubscriptionDoesNotRequireAPrimaryKeyguardCacheEntry() {
        val live = CarrierNameRules.Registration(0, 0)
        val registration = CarrierNameRules.registration(null, live)
        assertSame(live, registration)
        assertEquals("Second", CarrierNameRules.replacement(true, "Second", 1, 5,
            registration?.inService == true, false))
    }

    @Test fun freshRegistrationOverridesStaleCacheInBothDirections() {
        val registered = CarrierNameRules.Registration(0, 0)
        val noService = CarrierNameRules.Registration(1, 1)
        assertTrue(CarrierNameRules.registration(noService, registered)!!.inService)
        assertFalse(CarrierNameRules.registration(registered, noService)!!.inService)
        val emergency = CarrierNameRules.Registration(0, 0, emergencyOnly = true)
        assertFalse(CarrierNameRules.registration(registered, emergency)!!.inService)
    }

    @Test fun cacheIsOnlyAFallbackWhenSubscriptionReadIsUnavailable() {
        val cached = CarrierNameRules.Registration(0, 1)
        assertSame(cached, CarrierNameRules.registration(cached, null))
        assertNull(CarrierNameRules.registration(null, null))
    }

    @Test fun voiceAndDataRegistrationAreIndependentButEmergencyNeverBecomesAnAlias() {
        assertTrue(CarrierNameRules.Registration(0, null).inService)
        assertTrue(CarrierNameRules.Registration(0, 1).inService)
        assertTrue(CarrierNameRules.Registration(1, 0).inService)
        for (state in listOf(1, 2, 3)) {
            assertFalse(CarrierNameRules.Registration(state, null).inService)
            assertFalse(CarrierNameRules.Registration(state, state).inService)
        }
        assertFalse(CarrierNameRules.Registration(0, 0, emergencyOnly = true).inService)
    }

    @Test fun finalMergeKeepsHiddenAndEmergencyLabelsNative() {
        assertNull(CarrierNameRules.finalName(null, "Alias", null, false))
        assertEquals("", CarrierNameRules.finalName("", "Alias", null, false))
        val emergency = StringBuilder("Emergency calls only")
        assertSame(emergency, CarrierNameRules.finalName(emergency, "Alias", null, true))
        val native = StringBuilder("Carrier")
        assertSame(native, CarrierNameRules.finalName(native, null, null, false))
        assertSame(native, CarrierNameRules.finalName(native, "", null, false))
    }

    @Test fun vendorWfcReplacementIsOverriddenByPhysicalSlot() {
        val native = "Same carrier Wi-Fi"
        val names = CarrierNameSettings.State(true, "First", "Second")
        assertEquals("First", CarrierNameRules.finalName(native, names.name(0), "First", false))
        assertEquals("Second", CarrierNameRules.finalName(native, names.name(1), "Second", false))
    }

    @Test fun nativeDecorationSurvivesOnlyWhenTheRawAliasWasAppliedInThisFormatter() {
        val decorated = StringBuilder("Second 5G Wi-Fi Calling")
        assertSame(decorated, CarrierNameRules.finalName(decorated, "Second", "Second", false))
        assertEquals("Second", CarrierNameRules.finalName(decorated, "Second", null, false))
        assertEquals("Second", CarrierNameRules.finalName(decorated, "Second", "First", false))
        assertEquals("Second", CarrierNameRules.finalName("Vendor Wi-Fi", "Second", "Second", false))
    }
}
