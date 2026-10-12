package com.mi.onextbox.lsp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class SoftwareUpdateDiscoveryRulesTest {
    @Test fun otaOnlyStopsCachedTasteDiscoveryAtTheHandlerAndPreservesJobSetup() {
        for (message in 0..14000) {
            assertEquals(message == 1004, SoftwareUpdateDiscoveryRules.blocksOtaMessage(message, true))
            assertFalse(SoftwareUpdateDiscoveryRules.blocksOtaMessage(message, false))
        }
        assertEquals(2001, SoftwareUpdateDiscoveryRules.NO_OTA_UPDATE)
    }

    @Test fun inFlightVersionResponsesAreIgnoredWithoutDiscardingDescriptions() {
        assertTrue(SoftwareUpdateDiscoveryRules.blocksVersionResult("query_version_update", true))
        assertEquals(5, SoftwareUpdateDiscoveryRules.NO_VERSION_RESULT)
        for (query in listOf(null, "", "query_description", "query_version_update ", "queryOpexUpdate")) {
            assertFalse(SoftwareUpdateDiscoveryRules.blocksVersionResult(query, true))
        }
        assertFalse(SoftwareUpdateDiscoveryRules.blocksVersionResult("query_version_update", false))
    }

    @Test fun opexOnlyStopsNewDiscoveryNotDownloadOrInstallAndResultHandling() {
        for (task in -1..200) {
            assertEquals(task == 10, SoftwareUpdateDiscoveryRules.blocksOpexTask(task, true))
            assertFalse(SoftwareUpdateDiscoveryRules.blocksOpexTask(task, false))
        }
    }

    @Test fun opexAvailabilityIsHiddenButProgressAndUnknownStatusesArePreserved() {
        for (status in -5..100) {
            assertEquals(status in setOf(-1, 0), SoftwareUpdateDiscoveryRules.hidesOpexInfo("getOpexUpdateInfo", status, true))
            assertFalse(SoftwareUpdateDiscoveryRules.hidesOpexInfo("getOpexUpdateInfo", status, false))
            assertFalse(SoftwareUpdateDiscoveryRules.hidesOpexInfo("installOpex", status, true))
            assertFalse(SoftwareUpdateDiscoveryRules.hidesOpexInfo("queryOpexUpdate", status, true))
        }
        assertFalse(SoftwareUpdateDiscoveryRules.hidesOpexInfo("getOpexUpdateInfo", null, true))
    }

    @Test fun mergedOpexRequestDoesNotChangeOtaCotaSotaOrSecurityFields() {
        val original = """{"opex":{"check":true,"other":7},"components":[{"type":0}],"cota":{"version":1},"sota":{"version":2},"isRooted":true}"""
        val before = Json.parseToJsonElement(original) as JsonObject
        val after = Json.parseToJsonElement(SoftwareUpdateDiscoveryRules.disableMergedOpex(original, "/update/v6", true)) as JsonObject
        assertEquals(before - "opex", after - "opex")
        val opex = after.getValue("opex") as JsonObject
        assertEquals(JsonPrimitive(false), opex["check"])
        assertEquals(JsonPrimitive(7), opex["other"])
    }

    @Test fun noOtherRequestIsRewrittenAndDisabledModeIsByteIdentical() {
        val original = """{ "opex": { "check": true } }"""
        for (path in listOf(null, "", "/upgradeCheck", "/update/v60", "/update/v6/", "/system/file/update")) {
            assertEquals(original, SoftwareUpdateDiscoveryRules.disableMergedOpex(original, path, true))
        }
        assertEquals(original, SoftwareUpdateDiscoveryRules.disableMergedOpex(original, "/update/v6", false))
    }

    @Test fun missingOrUnexpectedOpexShapesArePreserved() {
        for (original in listOf("{}", "[]", "null", """{"opex":true}""", """{"opex":{"other":1}}""",
            """{"opex":{"check":{}}}""", """{"opex":{"check":null}}""", """{"opex":{"check":7}}""")) {
            assertEquals(original, SoftwareUpdateDiscoveryRules.disableMergedOpex(original, "/update/v6", true))
        }
    }
}
