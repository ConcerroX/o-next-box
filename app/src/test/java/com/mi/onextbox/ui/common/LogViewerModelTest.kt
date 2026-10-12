package com.mi.onextbox.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogViewerModelTest {
    @Test fun moduleRecordsKeepTheirStackTracesButNotOtherModules() {
        val lines = listOf(
            header("I", "Started"),
            header("W", "Hot reload preparation rejected"),
            "java.lang.IllegalStateException: Resource is busy: face-tap:session",
            "\tat com.mi.onextbox.lsp.OosLspModuleEntry.onHotReloading(Entry.kt:254)",
            header("E", "Other module secret", "com.example.other"),
            "\tat com.example.other.Private.method(Private.kt:1)",
            header("I", "Hot reload completed", process = "com.android.settings"),
        )
        val records = ModuleLogParser.parse(lines)
        assertEquals(3, records.size)
        assertEquals(AppLogLevel.WARN, records[1].level)
        assertTrue(records[1].raw.contains("face-tap:session"))
        assertTrue(records[1].message.contains("Entry.kt:254"))
        assertTrue(records.last().metadata.contains("com.android.settings"))
        assertEquals("com.android.settings", records.last().origin)
        assertEquals("2026-10-10 14:42:33.725", records.last().timestamp)
        assertTrue(records.first().message.startsWith("[Entry] Started"))
        assertFalse(records.any { it.raw.contains("secret") || it.raw.contains("Private.kt") })
    }

    @Test fun similarPackageNamesAndPartialLogTailsAreNotIncluded() {
        val records = ModuleLogParser.parse(listOf(
            "\tat com.mi.onextbox.Partial.stack(Partial.kt:1)",
            header("I", "Wrong package", "com.mi.onextbox.other"),
            header("I", "Quoted [com.mi.onextbox,ONextBox-LSP]", "com.example.other"),
            header("I", "Correct package"),
        ))
        assertEquals(1, records.size)
        assertFalse(records.single().raw.contains("Partial") || records.single().raw.contains("Wrong"))
    }

    @Test fun unknownFrameworkHeadersEndOurStackTrace() {
        val records = ModuleLogParser.parse(listOf(
            header("W", "Warning"),
            "\tat our.Stack.method(Stack.kt:1)",
            "[unknown-format] some other module",
            "\tat other.Stack.private(Stack.kt:2)",
        ))
        assertTrue(records.single().raw.contains("our.Stack"))
        assertFalse(records.single().raw.contains("other.Stack"))
    }

    @Test fun moduleLevelsIncludeVerboseAndFatal() {
        val records = ModuleLogParser.parse(listOf("V", "D", "I", "W", "E", "F").map { header(it, it) })
        assertEquals(listOf(AppLogLevel.DEBUG, AppLogLevel.DEBUG, AppLogLevel.INFO,
            AppLogLevel.WARN, AppLogLevel.ERROR, AppLogLevel.ERROR), records.map { it.level })
    }

    @Test fun moduleHistoryAndIndividualStackTracesAreBounded() {
        val records = ModuleLogParser.parse((0..510).map { header("I", "Record $it") })
        assertEquals(ModuleLogParser.MAX_RECORDS, records.size)
        assertTrue(records.first().message.endsWith("Record 11"))
        assertTrue(records.last().message.endsWith("Record 510"))
        assertEquals(records.size, records.map { it.key }.toSet().size)
        val long = ModuleLogParser.parse(listOf(header("E", "Failure"), "x".repeat(100_000)))
        assertTrue(long.single().raw.length <= 32_768)
    }

    @Test fun filteringMatchesMessageProcessAndStackWithoutChangingSourceOrder() {
        val records = ModuleLogParser.parse(listOf(
            header("I", "Ready", process = "com.android.settings"),
            header("W", "Rejected"), "java.lang.Exception: FACE-TAP busy",
            header("E", "Failed"),
        ))
        assertEquals(records.reversed(), filterLogRecords(records, LogLevelFilter.ALL, "  "))
        assertEquals(listOf(records[0]), filterLogRecords(records, LogLevelFilter.INFO, "Settings"))
        assertEquals(listOf(records[1]), filterLogRecords(records, LogLevelFilter.WARN, " face-tap "))
        assertTrue(filterLogRecords(records, LogLevelFilter.ERROR, "face-tap").isEmpty())
        assertEquals(AppLogLevel.INFO, records.first().level)
    }

    @Test fun applicationRecordsPreserveExportTextWhileDisplayingMultilineMessages() {
        val entry = AppLogEntry(42, 1_700_000_000_000, AppLogLevel.WARN, "LSPosed", "Failed\\nDetails")
        val record = entry.toLogRecord()
        assertEquals("app:42", record.key)
        assertEquals("Failed\nDetails", record.message)
        assertTrue(record.metadata.endsWith(" · LSPosed"))
        assertEquals("LSPosed", record.origin)
        assertEquals(entry.toLine().substringAfter('[').substringBefore(']'), record.timestamp)
        assertEquals(entry.toLine(), record.raw)
    }

    @Test fun emptyOrUnrelatedFrameworkLogsRemainEmpty() {
        assertTrue(ModuleLogParser.parse(emptyList()).isEmpty())
        assertTrue(ModuleLogParser.parse(listOf(header("I", "Other", "com.example.other"))).isEmpty())
    }

    private fun header(
        level: String,
        message: String,
        module: String = "com.mi.onextbox",
        process: String = "com.android.systemui",
    ) = "[ 2026-10-10T14:42:33.725    10264: 5596: 10368 $level/LSPosedFramework ] " +
        "($process)[$module,ONextBox-LSP,session,0,1] [Entry] $message"
}
