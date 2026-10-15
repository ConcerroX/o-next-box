package com.mi.onextbox.ui.common

import org.junit.Assert.*
import org.junit.Test

class HookEvidenceRecordsTest {
    private fun record(state: String, pid: Int = 10) = LogRecord("key", null, "",
        "[HookEvidence] feature=stacked_signal state=$state pid=$pid", "", timestamp = "2026-10-16 10:00")
    @Test fun evidenceDoesNotMergeProcessRestartsOrTreatInstallAsExecution() {
        val rows = hookEvidenceRecords(listOf(record("installed"), record("observed"), record("installed", 11)))
        assertEquals(listOf("installed", "observed"), rows.map { it.state })
        assertEquals(listOf("11", "10"), rows.map { it.pid })
    }
    @Test fun unsupportedOrMissingEvidenceIsNotAssumedSuccessful() {
        assertTrue(hookEvidenceRecords(listOf(record("unknown"))).isEmpty())
        assertTrue(hookEvidenceRecords(emptyList()).isEmpty())
        assertEquals("unavailable", hookEvidenceRecords(listOf(record("unavailable"))).single().state)
    }
    @Test fun historyIsBounded() {
        assertEquals(8, hookEvidenceRecords((1..20).map { record("observed", it) }).size)
    }
}
