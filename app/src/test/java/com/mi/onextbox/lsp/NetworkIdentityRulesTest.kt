package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class NetworkIdentityRulesTest {
    private val tw = NetworkIdentityRules.Profile("tw", "46692", "Chunghwa Telecom")
    private val us = NetworkIdentityRules.Profile("us", "310260", "T-Mobile")
    private fun native() = mapOf("gsm.sim.operator.numeric" to "46000,46001", "gsm.sim.operator.alpha" to "CMCC,Unicom",
        "gsm.sim.operator.iso-country" to "cn,cn", "gsm.operator.numeric" to "46000,", "gsm.operator.alpha" to "CMCC,",
        "gsm.operator.iso-country" to "cn,")

    @Test fun disabledAndMalformedProfilesAreNoOps() {
        assertEquals(native(), NetworkIdentityRules.transform(native(), null))
        for (bad in listOf(tw.copy(region = "xx"), tw.copy(numeric = "310260"), tw.copy(name = "\n"), us.copy(numeric = "31026"))) {
            assertFalse(NetworkIdentityRules.valid(bad))
            assertEquals(native(), NetworkIdentityRules.transform(native(), bad))
        }
    }
    @Test fun bothSimsChangeButMissingNetworkSlotsStayEmpty() {
        val result = NetworkIdentityRules.transform(native(), tw)
        assertEquals("46692,46692", result["gsm.sim.operator.numeric"])
        assertEquals("tw,tw", result["gsm.sim.operator.iso-country"])
        assertEquals("46692,", result["gsm.operator.numeric"])
        assertEquals("Chunghwa Telecom,", result["gsm.operator.alpha"])
    }
    @Test fun allMissingSimsStayMissing() {
        val empty = NetworkIdentityRules.properties.associateWith { "" }
        assertEquals(empty, NetworkIdentityRules.transform(empty, tw))
    }
    @Test fun leadingZerosAndEscapesRoundTrip() {
        val parts = listOf("", "AT&T, Inc.", "Back\\Slash", "")
        assertEquals(parts, NetworkIdentityRules.decode(NetworkIdentityRules.encode(parts)))
        assertTrue(NetworkIdentityRules.valid(tw.copy(numeric = "46601")))
        assertEquals("46601,46601", NetworkIdentityRules.transform(native(), tw.copy(numeric = "46601"))["gsm.sim.operator.numeric"])
    }
    @Test fun switchThenDisableRestoresBothOriginalSims() {
        val values = native().toMutableMap()
        var saved = emptyMap<String, NetworkIdentityProperties.Record>()
        val engine = NetworkIdentityProperties({ values.getValue(it) }, { k, v -> values[k] = v }, { saved = it.toMap() })
        engine.refresh(tw)
        assertEquals("46000,46001", engine.actual("gsm.sim.operator.numeric", values.getValue("gsm.sim.operator.numeric")))
        engine.refresh(us)
        assertEquals("310260,310260", values["gsm.sim.operator.numeric"])
        engine.refresh(null)
        assertEquals(native(), values)
        assertTrue(saved.isEmpty())
    }
    @Test fun externalWriterIsNotOverwrittenByRestore() {
        val values = native().toMutableMap()
        val engine = NetworkIdentityProperties({ values.getValue(it) }, { k, v -> values[k] = v }, {})
        engine.refresh(tw)
        values["gsm.sim.operator.numeric"] = "45400,"
        engine.refresh(null)
        assertEquals("45400,", values["gsm.sim.operator.numeric"])
    }
    @Test fun nativeUpdatesBecomeLatestRestoreValue() {
        val values = native().toMutableMap()
        val engine = NetworkIdentityProperties({ values.getValue(it) }, { k, v -> values[k] = v }, {})
        engine.refresh(tw)
        values["gsm.sim.operator.numeric"] = "45400,"
        engine.afterNativeWrite("gsm.sim.operator.numeric", "45400,")
        engine.refresh(tw)
        engine.refresh(null)
        assertEquals("45400,", values["gsm.sim.operator.numeric"])
    }
    @Test fun restartDuringAnyWriteCanRestore() {
        for (failureIndex in 0..5) {
            val values = native().toMutableMap()
            var saved = emptyMap<String, NetworkIdentityProperties.Record>()
            val initial = NetworkIdentityProperties({ values.getValue(it) }, { k, v -> values[k] = v }, { saved = it.toMap() })
            initial.refresh(tw)
            var writes = 0
            val failing = NetworkIdentityProperties({ values.getValue(it) }, { k, v ->
                if (writes++ == failureIndex) error("simulated process death")
                values[k] = v
            }, { saved = it.toMap() }, saved)
            runCatching { failing.refresh(us) }
            val recovered = NetworkIdentityProperties({ values.getValue(it) }, { k, v -> values[k] = v }, { saved = it.toMap() }, saved)
            recovered.refresh(null)
            assertEquals("failure at $failureIndex", native(), values)
            assertTrue(saved.isEmpty())
        }
    }
    @Test fun backupFailurePreventsAnyNewWrite() {
        var writes = 0
        val engine = NetworkIdentityProperties({ native().getValue(it) }, { _, _ -> writes++ }, { error("disk full") })
        assertTrue(runCatching { engine.refresh(tw) }.isFailure)
        assertEquals(0, writes)
    }
    @Test fun unchangedStateDoesNotWriteOrPersistAgain() {
        val values = native().toMutableMap()
        var persists = 0
        val engine = NetworkIdentityProperties({ values.getValue(it) }, { k, v -> values[k] = v }, { persists++ })
        engine.refresh(tw)
        val count = persists
        repeat(10) { engine.refresh(tw) }
        assertEquals(count, persists)
    }
    @Test fun failedNativeWriteDoesNotBecomeRestoreValue() {
        val values = native().toMutableMap()
        val engine = NetworkIdentityProperties({ values.getValue(it) }, { k, v -> values[k] = v }, {})
        engine.refresh(tw)
        runCatching {
            val nativeWriter: () -> Unit = { error("platform rejected update") }
            nativeWriter()
            engine.afterNativeWrite("gsm.sim.operator.numeric", "45400,")
        }
        engine.refresh(null)
        assertEquals(native(), values)
    }
    @Test fun oversizedPropertyAbortsWholeTransaction() {
        val values = native().toMutableMap().apply { this["gsm.sim.operator.numeric"] = "46000,46001,46000" }
        var writes = 0
        val engine = NetworkIdentityProperties({ values.getValue(it) }, { _, _ -> writes++ }, {})
        assertTrue(runCatching { engine.refresh(tw.copy(name = "a".repeat(40))) }.isFailure)
        assertEquals(0, writes)
    }
    @Test fun silentlyIgnoredWriteIsDetectedAndRecoverable() {
        val values = native().toMutableMap()
        var saved = emptyMap<String, NetworkIdentityProperties.Record>()
        val engine = NetworkIdentityProperties({ values.getValue(it) }, { _, _ -> }, { saved = it.toMap() })
        assertTrue(runCatching { engine.refresh(tw) }.isFailure)
        val recovered = NetworkIdentityProperties({ values.getValue(it) }, { k, v -> values[k] = v }, { saved = it.toMap() }, saved)
        recovered.refresh(null)
        assertEquals(native(), values)
        assertTrue(saved.isEmpty())
    }
    @Test fun catalogIsValidUniqueAndContainsPopularNetworks() {
        val file = File("src/main/assets/network_identity_catalog.tsv")
        val entries = file.bufferedReader().use { NetworkIdentityCatalog.parse(it.lineSequence()) }
        assertEquals(510, entries.size)
        assertEquals(entries.size, entries.map { it.numeric }.distinct().size)
        NetworkIdentityCatalog.popular.forEach { numeric -> assertTrue(numeric, entries.any { it.numeric == numeric }) }
        entries.forEach { assertTrue(it.toString(), NetworkIdentityRules.valid(NetworkIdentityRules.Profile(it.region, it.numeric, it.name))) }
    }
}
