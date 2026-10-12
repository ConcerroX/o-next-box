package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class LargeNetworkArtworkTest {
    @Test fun matchesBinaryNameWithoutDependingOnInnerClassMetadata() {
        assertTrue(LargeNetworkArtworkHooker.supportsModel(
            "com.android.systemui.statusbar.pipeline.mobile.domain.model.OplusNetworkTypeTextModel\$DefaultNetworkTypeText"))
        assertFalse(LargeNetworkArtworkHooker.supportsModel("DefaultNetworkTypeText"))
        assertFalse(LargeNetworkArtworkHooker.supportsModel("unrelated.DefaultNetworkTypeText"))
    }
    @Test fun mapsLteArtworkOnlyWhenRequested() {
        assertEquals("4g", LargeNetworkArtworkHooker.label("4G", false))
        assertEquals("4g_plus", LargeNetworkArtworkHooker.label("4G+", false))
        assertEquals("lte", LargeNetworkArtworkHooker.label("4G", true))
        assertEquals("lte_plus", LargeNetworkArtworkHooker.label("4G+", true))
    }
    @Test fun preservesOtherNetworksAndUnknownCarrierLabels() {
        for ((name, key) in mapOf("G" to "g", "E" to "e", "H" to "h", "H+" to "h_plus",
            "2G" to "2g", "2G+" to "2g_plus", "3G" to "3g", "3G+" to "3g_plus",
            "5G" to "5g", "5G+" to "5g_plus", "5G-A" to "5g_a", "LTE" to "lte", "LTE+" to "lte_plus")) {
            assertEquals(key, LargeNetworkArtworkHooker.label(name, true))
            assertEquals(key, LargeNetworkArtworkHooker.label(name, false))
        }
        assertEquals("5g_uw", LargeNetworkArtworkHooker.label("5G UW", true))
        assertNull(LargeNetworkArtworkHooker.label("unknown carrier", true))
        assertNull(LargeNetworkArtworkHooker.label(null, true))
        assertNull(LargeNetworkArtworkHooker.label("", true))
    }
    @Test fun extendedLabelsKeepTheirOwnIdentityWithLteEnabled() {
        val labels = mapOf("1X" to "1x", "4.5G" to "4_5g", "4.5G+" to "4_5g_plus",
            "5.5G" to "5_5g", "5G++" to "5g_plus_plus", "5GE" to "5g_e",
            "5GUC" to "5g_uc", "5G UC" to "5g_uc", "5GUW" to "5g_uw", "5GUWB" to "5g_uw",
            "TIGO_5G_CONNECTED" to "tigo_connected", "TIGO_5G_NOT_RESTRICTED" to "tigo_available")
        labels.forEach { (name, key) ->
            assertEquals(key, LargeNetworkArtworkHooker.label(name, false))
            assertEquals(key, LargeNetworkArtworkHooker.label(name, true))
        }
    }
}
