package com.mi.onextbox.errors

import org.junit.Assert.*
import org.junit.Test

class AppErrorMetadataTest {
    private val metadata = AppErrorMetadata(
        activity = "com.example.app/.MainActivity", parentActivity = "com.example.app/.Parent",
        parentProcess = "com.example.app", exceptionClass = "java.lang.IllegalStateException",
        exceptionMessage = "Sample failure\nMore details", throwFile = "Worker.kt", throwClass = "com.example.Worker",
        throwMethod = "run", throwLine = 27, targetSdk = 36, minSdk = 35, systemApp = false,
        device = "Test manufacturer Test device", androidVersion = "17", sdk = 37,
        fingerprint = "test/build/fingerprint", abi = "arm64-v8a", traceStatus = "captured",
    )

    @Test fun allMetadataSurvivesRoundTrip() {
        assertEquals(metadata, AppErrorMetadataCodec.decode(AppErrorMetadataCodec.encode(metadata)))
    }

    @Test fun olderAndBrokenMetadataNeverLoseTheRecord() {
        listOf(null, "", "{}", "not JSON", "[]", "{\"targetSdk\":\"bad\"}", "x".repeat(AppErrorMetadataCodec.MAX_ENCODED + 1))
            .forEach { assertEquals(AppErrorMetadata(), AppErrorMetadataCodec.decode(it)) }
        assertEquals(37, AppErrorMetadataCodec.decode("{\"sdk\":37,\"future\":true}").sdk)
    }

    @Test fun metadataIsBoundedBeforeEncodingAndAfterDecoding() {
        val input = metadata.copy(exceptionMessage = "x".repeat(100_000), fingerprint = "f".repeat(20_000), targetSdk = -1, sdk = 9999)
        val decoded = AppErrorMetadataCodec.decode(AppErrorMetadataCodec.encode(input))
        assertEquals(2048, decoded.exceptionMessage.length)
        assertEquals(1024, decoded.fingerprint.length)
        assertEquals(0, decoded.targetSdk)
        assertEquals(1000, decoded.sdk)
        assertTrue(AppErrorMetadataCodec.encode(input).length < AppErrorMetadataCodec.MAX_ENCODED)
    }

    @Test fun locationsPreserveFileAndLineAndMissingParts() {
        assertEquals("com.example.Worker.run · Worker.kt:27", metadata.location())
        assertEquals("Worker.kt", AppErrorMetadata(throwFile = "Worker.kt").location())
        assertEquals("", AppErrorMetadata().location())
    }

    @Test fun reportsIncludeSnapshotsAndDoNotInventUnknownFields() {
        val record = AppErrorRecord(time = 1, packageName = "com.example.app", processName = "com.example.app:worker", uid = 10123, pid = 123, kind = AppErrorKind.CRASH, summary = "Sample failure", detail = "Full stack", metadata = metadata)
        val report = record.report()
        listOf("Sample failure\nMore details", "Worker.kt:27", "Target SDK: 36", "Minimum SDK: 35", "User: 0", "test/build/fingerprint", "API: 37", "Full stack").forEach { assertTrue(it, report.contains(it)) }
        assertFalse(record.copy(metadata = AppErrorMetadata()).report().contains("Build fingerprint:"))
        assertFalse(requireNotNull(AppErrorRules.bounded(record, 0)).truncated)
        assertTrue(requireNotNull(AppErrorRules.bounded(record.copy(metadata = metadata.copy(exceptionMessage = "x".repeat(3000))), 0)).truncated)
    }

    @Test fun combinedDetailsPrioritizeTheStackAndIncludeTheSystemReport() {
        val (text, truncated) = AppErrorRules.combineDetails(listOf("Stack", null, "", "Reason", "System report", "Stack"))
        assertEquals("Stack\n\nReason\n\nSystem report", text)
        assertFalse(truncated)
        val (large, cut) = AppErrorRules.combineDetails(listOf("Stack".repeat(20_000), "Report"))
        assertEquals(AppErrorRules.MAX_DETAIL, large.length)
        assertTrue(large.startsWith("Stack"))
        assertTrue(cut)
        assertEquals("" to false, AppErrorRules.combineDetails(listOf(null, "")))
    }
}
