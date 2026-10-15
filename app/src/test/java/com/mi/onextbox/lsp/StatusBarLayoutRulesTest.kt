package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class StatusBarLayoutRulesTest {
    @Test fun initialOrderPreservesNativeC17PositionsIncludingMuteAndNetworkSpeed() {
        val native = listOf("logging", "hotspot", "data_saver", "cast", "vpn", "nfc", "headset", "alarm_clock",
            "volume", "bluetooth", "location", "rotate", "managed_profile", "mute", "network_speed", "zen",
            "stacked_mobile", "mobile", "nosim_all", "wifi", "dynamic_info", "airplane", "battery")
        assertEquals(native, StatusBarLayoutRules.reorder(native, StatusBarLayoutRules.defaultOrder))
    }
    @Test fun normalizationKeepsOnlyKnownUniqueGroupsAndRestoresMissingGroups() {
        val order = StatusBarLayoutRules.normalizeOrder(listOf("wifi", "wifi", "unknown", "mobile"))
        assertEquals(listOf("wifi", "mobile"), order.take(2))
        assertEquals(StatusBarLayoutRules.defaultOrder.toSet(), order.toSet())
        assertEquals(order.size, order.toSet().size)
    }
    @Test fun reorderingRetainsUnknownSlotsAndNativeSimOrder() {
        val slots = listOf("privacy", "mobile", "stacked_mobile", "unknown", "wifi", "bluetooth", "battery")
        val next = StatusBarLayoutRules.reorder(slots, listOf("wifi", "bluetooth", "mobile"))
        assertEquals(listOf("privacy", "wifi", "bluetooth", "unknown", "mobile", "stacked_mobile", "battery"), next)
        assertEquals(slots.sorted(), next.sorted())
        assertEquals("mobile", slots[1])
    }
    @Test fun duplicateSlotsAreNotLostOrDuplicated() {
        val slots = listOf("wifi", "mobile", "wifi", "unsupported")
        assertEquals(listOf("mobile", "wifi", "wifi", "unsupported"), StatusBarLayoutRules.reorder(slots, listOf("mobile", "wifi")))
        assertEquals(emptyList<String>(), StatusBarLayoutRules.reorder(emptyList(), emptyList()))
    }
    @Test fun marginsAreDensityBasedBoundedAndNeverCrossTheCutout() {
        assertEquals(40, StatusBarLayoutRules.padding(20, 10, 2f, 0))
        assertEquals(8, StatusBarLayoutRules.padding(20, -16, 2f, 8))
        assertEquals(100, StatusBarLayoutRules.padding(20, 500, 2f, 0))
        assertEquals(20, StatusBarLayoutRules.padding(20, 0, 2f, 0))
        assertEquals(20, StatusBarLayoutRules.padding(20, 10, Float.NaN, 0))
        assertEquals(20, StatusBarLayoutRules.padding(20, 10, 0f, 0))
    }
    @Test fun expandedDefaultsAndOldDefaultPreferencesDoNotMoveNativeIcons() {
        assertEquals(21, StatusBarLayoutRules.defaultOrder.size)
        assertFalse("battery" in StatusBarLayoutRules.defaultOrder)
        assertFalse("clock" in StatusBarLayoutRules.defaultOrder)
        assertEquals(StatusBarLayoutRules.defaultOrder, StatusBarLayoutRules.normalizeOrder(StatusBarLayoutRules.legacyDefaultOrder))
        assertEquals(StatusBarLayoutRules.defaultOrder, StatusBarLayoutRules.normalizeOrder(emptyList()))
    }
    @Test fun oldCustomOrderSurvivesWhileNewGroupsAreInserted() {
        val old = StatusBarLayoutRules.legacyDefaultOrder.reversed()
        val expanded = StatusBarLayoutRules.normalizeOrder(old)
        assertEquals(old, expanded.filter { it in StatusBarLayoutRules.legacyDefaultOrder })
        assertEquals(21, expanded.size)
        assertEquals(expanded, StatusBarLayoutRules.normalizeOrder(expanded))
    }
    @Test fun simNormalizationRejectsDuplicatesAndInvalidPhysicalSlots() {
        assertEquals(listOf(0, 1), StatusBarLayoutRules.normalizeSimOrder(emptyList()))
        assertEquals(listOf(1, 0), StatusBarLayoutRules.normalizeSimOrder(listOf(1, 1, 9, -1)))
    }
    @Test fun simOrderUsesPhysicalSlotsRatherThanTheSubscriptionIdMagnitude() {
        val ids = listOf(200, 7)
        val physicalSlots = mapOf(200 to 1, 7 to 0)
        assertEquals(listOf(7, 200), StatusBarLayoutRules.reorderSubscriptions(ids, true, physicalSlots, listOf(0, 1)))
        assertEquals(listOf(200, 7), StatusBarLayoutRules.reorderSubscriptions(ids, true, physicalSlots, listOf(1, 0)))
        assertEquals(listOf(200, 7), ids)
        assertSame(ids, StatusBarLayoutRules.reorderSubscriptions(ids, false, physicalSlots, listOf(0, 1)))
    }
    @Test fun unknownSubscriptionsSingleSimAndDuplicateIdsAreNotLost() {
        val slots = mapOf(5 to 1, 8 to 0, 9 to -1)
        assertEquals(listOf(8, 9, 5), StatusBarLayoutRules.reorderSubscriptions(listOf(5, 9, 8), true, slots, listOf(0, 1)))
        assertEquals(listOf(8, 5, 5), StatusBarLayoutRules.reorderSubscriptions(listOf(5, 8, 5), true, slots, listOf(0, 1)))
        assertEquals(listOf(5, 8), StatusBarLayoutRules.reorderSubscriptions(listOf(5, 8), true, emptyMap(), listOf(0, 1)))
        assertEquals(listOf(8), StatusBarLayoutRules.reorderSubscriptions(listOf(8), true, slots, listOf(1, 0)))
        assertEquals(emptyList<Int>(), StatusBarLayoutRules.reorderSubscriptions(emptyList(), true, slots, listOf(1, 0)))
    }
    @Test fun dataSimOnlyStillWinsAfterSortingAndNativeReverseCannotMutateTheSource() {
        val source = arrayListOf(5, 8)
        val sorted = StatusBarLayoutRules.reorderSubscriptions(source, true, mapOf(5 to 1, 8 to 0), listOf(0, 1))
        assertEquals(listOf(5), DataSimSignalRules.select(sorted, true, 5))
        assertEquals(listOf(8), DataSimSignalRules.select(sorted, true, 8))
        val native = ArrayList(sorted)
        native.reverse()
        assertEquals(listOf(5, 8), source)
        assertEquals(listOf(8, 5), sorted)
    }

    @Test fun verticalOffsetsUseNativePaddingDensityAndIndependentValues() {
        assertEquals(8 to 7, StatusBarLayoutRules.verticalPadding(6, 4, 2, 3, 1f, 40, 20))
        assertEquals(10 to 4, StatusBarLayoutRules.verticalPadding(6, 4, 2, 0, 2f, 40, 20))
        assertEquals(6 to 8, StatusBarLayoutRules.verticalPadding(6, 4, 0, 2, 2f, 40, 20))
        assertEquals(6 to 4, StatusBarLayoutRules.verticalPadding(6, 4, 0, 0, 3f, 40, 20))
    }

    @Test fun negativeVerticalOffsetsNeverMovePaddingOutsideTheWindow() {
        assertEquals(0 to 0, StatusBarLayoutRules.verticalPadding(6, 4, -16, -16, 2f, 40, 20))
        assertEquals(0 to 4, StatusBarLayoutRules.verticalPadding(6, 4, -500, 0, 2f, 40, 20))
    }

    @Test fun oversizedVerticalOffsetsShareSpaceWithoutErasingTheNativeIconRow() {
        val result = StatusBarLayoutRules.verticalPadding(6, 4, 40, 40, 2f, 40, 20)
        assertEquals(10 to 10, result)
        assertEquals(20, 40 - result.first - result.second)
        val down = StatusBarLayoutRules.verticalPadding(6, 4, 40, 0, 2f, 40, 20)
        val up = StatusBarLayoutRules.verticalPadding(6, 4, 0, 40, 2f, 40, 20)
        assertTrue(down.first > down.second)
        assertTrue(up.second > up.first)
    }

    @Test fun verticalLimitsAndExistingNativeClippingDoNotChangeZeroOffsets() {
        assertEquals(6 to 4, StatusBarLayoutRules.verticalPadding(6, 4, 0, 0, 2f, 20, 30))
        assertEquals(StatusBarLayoutRules.verticalPadding(6, 4, 40, -16, 2f, 40, 20),
            StatusBarLayoutRules.verticalPadding(6, 4, Int.MAX_VALUE, Int.MIN_VALUE, 2f, 40, 20))
    }

    @Test fun invalidOrUnavailableVerticalMetricsKeepNativeGeometry() {
        for (density in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(6 to 4, StatusBarLayoutRules.verticalPadding(6, 4, 5, 5, density, 40, 20))
        }
        assertEquals(6 to 4, StatusBarLayoutRules.verticalPadding(6, 4, 5, 5, 2f, 0, 20))
        assertEquals(6 to 4, StatusBarLayoutRules.verticalPadding(6, 4, 5, 5, 2f, 40, 0))
        assertEquals(6 to 4, StatusBarLayoutRules.verticalPadding(6, 4, 5, 5, 2f, 10, 20))
        assertEquals(-1 to 4, StatusBarLayoutRules.verticalPadding(-1, 4, 5, 5, 2f, 40, 20))
    }

    @Test fun rotatedDisplaysFontSizesAndExtremeInputsStayWithinCurrentHeight() {
        for (height in listOf(32, 40, 100, Int.MAX_VALUE)) {
            for (icon in listOf(16, 24, 100)) {
                for (density in listOf(1f, 2.75f, 4f, Float.MAX_VALUE)) {
                    for (top in listOf(-16, 0, 5, 40)) for (bottom in listOf(-16, 0, 5, 40)) {
                        val padding = StatusBarLayoutRules.verticalPadding(6, 4, top, bottom, density, height, icon)
                        assertTrue(padding.first >= 0 && padding.second >= 0)
                        assertTrue(height.toLong() - padding.first - padding.second >= minOf(icon, height - 10))
                    }
                }
            }
        }
    }

    @Test fun eachRecalculationStartsFromNativePaddingNotThePreviousOverride() {
        val first = StatusBarLayoutRules.verticalPadding(6, 4, 2, 3, 1f, 40, 20)
        repeat(10) { assertEquals(first, StatusBarLayoutRules.verticalPadding(6, 4, 2, 3, 1f, 40, 20)) }
        assertEquals(6 to 4, StatusBarLayoutRules.verticalPadding(6, 4, 0, 0, 1f, 40, 20))
    }

    @Test fun freshInstallLeavesBothAxesOffWithZeroVerticalOffsets() {
        val state = StatusBarLayoutSettings.State()
        assertFalse(state.marginsEnabled)
        assertFalse(state.verticalMarginsEnabled)
        assertEquals(0, state.top)
        assertEquals(0, state.bottom)
        assertFalse(state.copy(marginsEnabled = true).verticalMarginsEnabled)
        assertFalse(state.copy(verticalMarginsEnabled = true).marginsEnabled)
    }
}
