package com.mi.onextbox.errors

import org.junit.Assert.*
import org.junit.Test

class AppErrorRulesTest {
    private fun record() = AppErrorRecord(time = 1, packageName = "com.example.app", processName = "com.example.app:worker", uid = 10123, pid = 123, kind = AppErrorKind.CRASH, summary = "IllegalStateException: sample", detail = "at com.example.Worker.run(Worker.kt:12)")

    @Test fun acceptsOwnUserAndKeepsProcessDetails() {
        val input = record()
        assertEquals(input, AppErrorRules.bounded(input, 0))
    }

    @Test fun rejectsCrossProfileReports() {
        assertNull(AppErrorRules.bounded(record().copy(uid = 110123), 0))
        assertNotNull(AppErrorRules.bounded(record().copy(uid = 110123), 1))
    }

    @Test fun rejectsBadIdentityAndTime() {
        listOf(record().copy(uid = -1), record().copy(pid = -1), record().copy(time = 0),
            record().copy(packageName = "../app"), record().copy(packageName = ""),
            record().copy(processName = "worker\nspoof")).forEach { assertNull(AppErrorRules.bounded(it, 0)) }
    }

    @Test fun onlySystemMayWriteAndOnlyOwnerMayRead() {
        assertTrue(AppErrorRules.canWrite(1000))
        listOf(0, 2000, 10123, 101000).forEach { assertFalse(AppErrorRules.canWrite(it)) }
        assertTrue(AppErrorRules.canRead(10123, 10123))
        listOf(0, 1000, 2000, 10124, 110123).forEach { assertFalse(AppErrorRules.canRead(it, 10123)) }
    }

    @Test fun boundsStacksAndSummariesAndMarksTruncation() {
        val bounded = requireNotNull(AppErrorRules.bounded(record().copy(detail = "a".repeat(100_000), summary = "b".repeat(2000)), 0))
        assertEquals(AppErrorRules.MAX_DETAIL, bounded.detail.length)
        assertEquals(AppErrorRules.MAX_SUMMARY, bounded.summary.length)
        assertTrue(bounded.truncated)
    }

    @Test fun sanitizesNulWithoutLosingStackNewlines() {
        val bounded = requireNotNull(AppErrorRules.bounded(record().copy(detail = "line\u0000\nline"), 0))
        assertEquals("line \nline", bounded.detail)
    }

    @Test fun classifiesNativeJavaAndAnrWithoutRecordingWtf() {
        assertEquals(AppErrorKind.CRASH, AppErrorRules.kind("crash", "java.lang.NullPointerException"))
        assertEquals(AppErrorKind.NATIVE, AppErrorRules.kind("native_crash", null))
        assertEquals(AppErrorKind.NATIVE, AppErrorRules.kind("crash", "Native crash"))
        assertEquals(AppErrorKind.ANR, AppErrorRules.kind("anr", null))
        assertNull(AppErrorRules.kind("wtf", null))
        assertNull(AppErrorRules.kind(null, null))
    }

    @Test fun searchRequiresAllTermsAndPreservesRecency() {
        val first = record().copy(id = 2)
        val second = record().copy(id = 1, summary = "NullPointerException")
        assertEquals(listOf(first), AppErrorRules.filter(listOf(first, second), null, "WORKER illegal"))
        assertEquals(listOf(first, second), AppErrorRules.filter(listOf(first, second), null, "  "))
    }

    @Test fun kindFilterIsIndependentFromSearch() {
        val first = record()
        val second = record().copy(kind = AppErrorKind.ANR)
        assertEquals(listOf(second), AppErrorRules.filter(listOf(first, second), AppErrorKind.ANR, "example"))
    }

    @Test fun applicationLabelsAreSearchableAndBounded() {
        val input = record().copy(appName = "测试应用", version = "1.2 (123)")
        assertEquals(listOf(input), AppErrorRules.filter(listOf(input), null, "测试"))
        assertTrue(input.report().contains("Version: 1.2 (123)"))
        val bounded = requireNotNull(AppErrorRules.bounded(input.copy(appName = "x".repeat(500), version = "1\n2\u0000"), 0))
        assertEquals(255, bounded.appName.length)
        assertEquals("1 2 ", bounded.version)
    }

    @Test fun reportIncludesIdentityAndTruncationMarker() {
        val text = record().copy(truncated = true).report()
        listOf("com.example.app", "com.example.app:worker", "UID: 10123", "PID: 123", "Worker.kt:12", "[TRUNCATED]").forEach { assertTrue(text.contains(it)) }
    }

    @Test fun frameworkSignaturesValidateTheWholeStablePrefix() {
        val prefix = listOf("java.lang.String", "com.android.server.am.ProcessRecord", "java.lang.String",
            "java.lang.String", "java.lang.String", "com.android.server.am.ProcessRecord", "java.lang.String",
            "java.lang.String", "java.io.File", "android.app.ApplicationErrorReport\$CrashInfo")
        assertTrue(AppErrorRules.supportsSignature(prefix))
        assertTrue(AppErrorRules.supportsSignature(prefix + List(5) { "java.lang.Object" }))
        assertFalse(AppErrorRules.supportsSignature(prefix.dropLast(1)))
        assertFalse(AppErrorRules.supportsSignature(prefix.toMutableList().apply { this[8] = "java.lang.String" }))
        assertFalse(AppErrorRules.supportsSignature(prefix + List(7) { "java.lang.Object" }))
    }
}
