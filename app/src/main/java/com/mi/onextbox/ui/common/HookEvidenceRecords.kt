package com.mi.onextbox.ui.common

internal data class HookEvidenceRecord(val feature: String, val state: String, val pid: String, val timestamp: String)

/** Historical evidence from the bounded module-log tail, never an inferred live Hook status. */
internal fun hookEvidenceRecords(records: List<LogRecord>): List<HookEvidenceRecord> {
    val pattern = Regex("\\[HookEvidence] feature=(stacked_signal|qs_corner) state=(installed|observed|unavailable) pid=([0-9]+)$")
    val latest = linkedMapOf<Pair<String, String>, HookEvidenceRecord>()
    for (record in records) {
        val match = pattern.find(record.message.lineSequence().firstOrNull().orEmpty()) ?: continue
        val (feature, state, pid) = match.destructured
        // Include timestamp in display and PID in grouping; do not merge restarted processes.
        val key = feature to pid
        latest.remove(key)
        latest[key] = HookEvidenceRecord(feature, state, pid, record.timestamp)
    }
    return latest.values.toList().takeLast(8).reversed()
}
