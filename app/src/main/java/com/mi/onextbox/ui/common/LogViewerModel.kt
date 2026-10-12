package com.mi.onextbox.ui.common

internal enum class LogSource { APP, MODULE }
internal enum class LogLevelFilter { ALL, DEBUG, INFO, WARN, ERROR }

internal data class LogRecord(
    val key: String,
    val level: AppLogLevel?,
    val metadata: String,
    val message: String,
    val raw: String,
    val timestamp: String = "",
    val origin: String = "",
)

internal fun AppLogEntry.toLogRecord(): LogRecord {
    val raw = toLine()
    return LogRecord(
        key = "app:$id",
        level = level,
        metadata = raw.substringBefore(" [$tag]") + " · $tag",
        message = message.replace("\\n", "\n"),
        raw = raw,
        timestamp = raw.substringAfter('[').substringBefore(']'),
        origin = tag,
    )
}

internal fun filterLogRecords(
    records: List<LogRecord>,
    filter: LogLevelFilter,
    query: String,
): List<LogRecord> {
    val keyword = query.trim()
    return records.asReversed().filter { record ->
        (filter == LogLevelFilter.ALL || record.level?.name == filter.name) &&
            (keyword.isEmpty() || record.raw.contains(keyword, ignoreCase = true))
    }
}

/** Keep only this module's records and their following stack traces, never other modules' records. */
internal object ModuleLogParser {
    const val MAX_RECORDS = 500
    private const val MAX_RECORD_LENGTH = 32_768
    private val header = Regex("""^\[\s*(\S+)\s+.*?\b([VDIWEF])/[^]]+]\s*(.*)$""")
    private val process = Regex("""^\(([^)]+)\)""")
    private val owner = Regex("""^(?:\([^)]+\)\s*)?\[com\.mi\.onextbox,""")

    fun parse(lines: List<String>): List<LogRecord> {
        val records = ArrayDeque<LogRecord>()
        var current: LogRecord? = null
        val continuation = StringBuilder()

        fun finish() {
            current?.let { record ->
                val suffix = continuation.toString().trimEnd()
                records.addLast(record.copy(message = record.message + suffix, raw = record.raw + suffix))
                if (records.size > MAX_RECORDS) records.removeFirst()
            }
            current = null
            continuation.clear()
        }

        lines.forEachIndexed { index, line ->
            val match = header.matchEntire(line)
            if (match != null) {
                finish()
                val body = match.groupValues[3]
                if (owner.containsMatchIn(body)) {
                    val level = when (match.groupValues[2]) {
                        "V", "D" -> AppLogLevel.DEBUG
                        "I" -> AppLogLevel.INFO
                        "W" -> AppLogLevel.WARN
                        else -> AppLogLevel.ERROR
                    }
                    val name = process.find(body)?.groupValues?.get(1) ?: "LSPosed"
                    val text = body.substringAfter("] ", body).take(MAX_RECORD_LENGTH)
                    current = LogRecord(
                        key = "module:$index:${line.hashCode()}",
                        level = level,
                        metadata = "${match.groupValues[1]} · ${level.name} · $name",
                        message = text,
                        raw = line.take(MAX_RECORD_LENGTH),
                        timestamp = match.groupValues[1].replace('T', ' '),
                        origin = name,
                    )
                }
            } else if (line.startsWith('[')) {
                // Unknown framework formats must not attach another module's output to ours.
                finish()
            } else if (current != null) {
                val remaining = MAX_RECORD_LENGTH - current!!.raw.length - continuation.length
                if (remaining > 1) continuation.append('\n').append(line.take(remaining - 1))
            }
        }
        finish()
        return records.toList()
    }
}
