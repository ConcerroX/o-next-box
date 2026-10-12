package com.mi.onextbox.lsp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class SoftwareUpdateRulesTest {
    @Test fun allDisabledActionsPassThrough() {
        listOf(null, "", "oplus.intent.action.OTA_AUTO_DOWNLOAD", "oplus.intent.action.OTA_PUSH_AUTO_DOWNLOAD",
            "oplus.intent.action.OTA_DELAY_AUTO_INSTALL").forEach {
            assertFalse(SoftwareUpdateRules.blocksAction(it, false, false))
        }
    }

    @Test fun automaticDownloadAndNightInstallationAreIndependent() {
        for (action in listOf("oplus.intent.action.OTA_AUTO_DOWNLOAD", "oplus.intent.action.OTA_PUSH_AUTO_DOWNLOAD")) {
            assertTrue(SoftwareUpdateRules.blocksAction(action, true, false))
            assertFalse(SoftwareUpdateRules.blocksAction(action, false, true))
        }
        assertTrue(SoftwareUpdateRules.blocksAction("oplus.intent.action.OTA_DELAY_AUTO_INSTALL", false, true))
        assertFalse(SoftwareUpdateRules.blocksAction("oplus.intent.action.OTA_DELAY_AUTO_INSTALL", true, false))
    }

    @Test fun manualActionsAndUpdateResultsAreNeverBlocked() {
        listOf("oplus.intent.action.OTA_START_DOWNLOAD", "oplus.intent.action.OTA_DOWNLOAD_RETRY",
            "oplus.intent.action.OTA_PAUSE_DOWNLOAD", "oplus.intent.action.OTA_REPORT_INSTALL_RESULT",
            "oplus.intent.ota.CHECK_NEW_VERSION_UPDATE", "com.oplus.ota.action.HANDLE_UPLOAD_MAINLINE_SESSION",
            "oplus.intent.action.OTA_AUTO_DOWNLOAD_EXTRA", "oplus.intent.action.OTA_DELAY_AUTO_INSTALL_EXTRA")
            .forEach { assertFalse(it, SoftwareUpdateRules.blocksAction(it, true, true)) }
    }

    @Test fun queuedAutomaticRequestsAreStoppedButManualInstallationContinues() {
        assertTrue(SoftwareUpdateRules.blocksMessage(3001, false, true, false))
        assertFalse(SoftwareUpdateRules.blocksMessage(3001, false, false, true))
        assertTrue(SoftwareUpdateRules.blocksMessage(6001, false, false, true))
        for (message in 6002..6003) {
            assertTrue(SoftwareUpdateRules.blocksMessage(message, true, false, true))
            assertFalse(SoftwareUpdateRules.blocksMessage(message, false, true, true))
        }
    }

    @Test fun progressAndAlreadyStartedInstallationAreNotInterrupted() {
        for (message in listOf(0, 1001, 2001, 3002, 6004, 7003, 10003, 10012)) {
            assertFalse(SoftwareUpdateRules.blocksMessage(message, true, true, true))
        }
        for (message in listOf(3001, 6001, 6002, 6003)) {
            assertFalse(SoftwareUpdateRules.blocksMessage(message, true, false, false))
        }
    }

    @Test fun rootReportPreservesTheOfficialWireType() {
        listOf("true" to "false", "false" to "false", "1" to "0", "0" to "0",
            "\"1\"" to "\"0\"", "\"0\"" to "\"0\"", "\"true\"" to "\"false\"",
            "\"false\"" to "\"false\"").forEach { (input, expected) ->
            assertEquals("{\"isRooted\":$expected}", SoftwareUpdateRules.maskRootReport("{\"isRooted\":$input}"))
        }
    }

    @Test fun nestedReportsAreMaskedWithoutChangingFullPackageStrategyOrBootloaderState() {
        val original = Json.parseToJsonElement("""{
            "isRooted":"1", "type":"1", "isLocked":false, "isUnlocked":true, "isUnlock":"1",
            "last_install_failed":true, "appIsRooted":true,
            "eventInfo":{"isRooted":true,"errorKey":"123","errorMsg":"Root detected"},
            "list":[{"isRooted":1}], "attestation":{"certificate":"signed","payload":"unchanged"}
        }""") as JsonObject
        val changed = Json.parseToJsonElement(SoftwareUpdateRules.maskRootReport(original.toString())) as JsonObject
        assertEquals(JsonPrimitive("0"), changed["isRooted"])
        assertEquals(JsonPrimitive(false), (changed["eventInfo"] as JsonObject)["isRooted"])
        assertEquals(JsonPrimitive(0), ((changed["list"] as JsonArray)[0] as JsonObject)["isRooted"])
        for (key in listOf("type", "isLocked", "isUnlocked", "isUnlock", "last_install_failed", "appIsRooted", "attestation")) {
            assertEquals(key, original[key], changed[key])
        }
        assertEquals(JsonPrimitive("123"), (changed["eventInfo"] as JsonObject)["errorKey"])
    }

    @Test fun unexpectedRootFieldsAndUnrelatedPayloadsAreUntouched() {
        for (value in listOf("null", "\"unknown\"", "17", "[]", "{}")) {
            val text = "{\"isRooted\":$value,\"root\":true,\"isRoot\":true}"
            assertEquals(Json.parseToJsonElement(text), Json.parseToJsonElement(SoftwareUpdateRules.maskRootReport(text)))
        }
        assertEquals("{\"value\":\"isRooted:true\"}", SoftwareUpdateRules.maskRootReport("{\"value\":\"isRooted:true\"}"))
    }

    @Test fun excessiveDepthFailsBeforeAnythingIsForwarded() {
        assertThrows(IllegalArgumentException::class.java) {
            SoftwareUpdateRules.maskRootReport("[".repeat(40) + "1" + "]".repeat(40))
        }
    }

    @Test fun rootAndBootloaderReportSwitchesAreIndependent() {
        val text = """{"isRooted":true,"isLocked":false,"isUnlocked":"true","isUnlock":1,"type":"1","appIsRooted":true,"last_install_failed":true}"""
        val original = Json.parseToJsonElement(text) as JsonObject
        for (root in listOf(false, true)) for (bootloader in listOf(false, true)) {
            val changed = Json.parseToJsonElement(SoftwareUpdateRules.maskSecurityReport(text, root, bootloader)) as JsonObject
            assertEquals(JsonPrimitive(!root), changed["isRooted"])
            assertEquals(JsonPrimitive(bootloader), changed["isLocked"])
            assertEquals(JsonPrimitive((!bootloader).toString()), changed["isUnlocked"])
            assertEquals(JsonPrimitive(if (bootloader) 0 else 1), changed["isUnlock"])
            for (key in listOf("type", "appIsRooted", "last_install_failed")) assertEquals(key, original[key], changed[key])
        }
        assertEquals(text, SoftwareUpdateRules.maskSecurityReport(text, false, false))
    }

    @Test fun bootloaderReportsRetainAllSupportedWireTypes() {
        for (key in listOf("isLocked", "isUnlocked", "isUnlock")) {
            val locked = key == "isLocked"
            listOf("true" to locked.toString(), "false" to locked.toString(), "1" to if (locked) "1" else "0",
                "0" to if (locked) "1" else "0", "\"1\"" to if (locked) "\"1\"" else "\"0\"",
                "\"0\"" to if (locked) "\"1\"" else "\"0\"", "\"true\"" to "\"$locked\"",
                "\"false\"" to "\"$locked\"").forEach { (input, expected) ->
                assertEquals("{\"$key\":$expected}", SoftwareUpdateRules.maskSecurityReport("{\"$key\":$input}", false, true))
            }
        }
    }

    @Test fun unexpectedBootloaderFieldsAndSignedAttestationAreNeverRewritten() {
        for (value in listOf("null", "\"unknown\"", "17", "[]", "{}")) {
            val text = "{\"isLocked\":$value,\"isUnlocked\":$value,\"isUnlock\":$value,\"device_state\":\"unlocked\"}"
            assertEquals(Json.parseToJsonElement(text), Json.parseToJsonElement(SoftwareUpdateRules.maskSecurityReport(text, true, true)))
        }
        val text = """{"isLocked":false,"reports":[{"isRooted":true,"isLocked":false}],"attestation":{"isLocked":false,"isRooted":true},"attestationInfo":{"isUnlock":"1"},"signedPayload":{"isUnlocked":true},"signature":"keep","certificate":"keep"}"""
        val original = Json.parseToJsonElement(text) as JsonObject
        val changed = Json.parseToJsonElement(SoftwareUpdateRules.maskSecurityReport(text, true, true)) as JsonObject
        assertEquals(JsonPrimitive(true), changed["isLocked"])
        assertEquals(JsonPrimitive(true), ((changed["reports"] as JsonArray)[0] as JsonObject)["isLocked"])
        assertEquals(JsonPrimitive(false), ((changed["reports"] as JsonArray)[0] as JsonObject)["isRooted"])
        for (key in listOf("attestation", "attestationInfo", "signedPayload", "signature", "certificate")) {
            assertEquals(key, original[key], changed[key])
        }
    }

    @Test fun bootloaderRewriteAlsoRejectsOversizedOrInvalidStructuredReports() {
        assertThrows(IllegalArgumentException::class.java) {
            SoftwareUpdateRules.maskSecurityReport("[".repeat(40) + "1" + "]".repeat(40), false, true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SoftwareUpdateRules.maskSecurityReport("[" + "0,".repeat(8192) + "0]", false, true)
        }
        assertThrows(kotlinx.serialization.SerializationException::class.java) {
            SoftwareUpdateRules.maskSecurityReport("{broken", false, true)
        }
    }

    @Test fun onlyUsableHttpLinksCanBeCopied() {
        for (value in listOf(null, "", "file:///data/ota.zip", "content://com.oplus.ota/pkgList", "javascript:alert(1)",
            "https:///missing-host", "https://user:secret@example.test/update", "https://bad host/update")) {
            assertNull(value, SoftwareUpdateRules.validDownloadUrl(value))
        }
        assertEquals("https://example.test/update.zip?signature=token", SoftwareUpdateRules.validDownloadUrl(" https://example.test/update.zip?signature=token "))
        assertEquals("http://example.test/update.zip", SoftwareUpdateRules.validDownloadUrl("http://example.test/update.zip"))
    }
}
