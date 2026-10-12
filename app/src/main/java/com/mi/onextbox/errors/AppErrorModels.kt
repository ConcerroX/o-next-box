package com.mi.onextbox.errors

enum class AppErrorKind { CRASH, NATIVE, ANR }

data class AppErrorRecord(
    val id: Long = 0,
    val time: Long,
    val packageName: String,
    val processName: String,
    val uid: Int,
    val pid: Int,
    val kind: AppErrorKind,
    val summary: String,
    val detail: String = "",
    val truncated: Boolean = false,
    val appName: String = "",
    val version: String = "",
    val metadata: AppErrorMetadata = AppErrorMetadata(),
) {
    fun report(): String = buildString {
        appendLine("ONextBox · ${kind.name}")
        appendLine("Time: ${java.time.Instant.ofEpochMilli(time)}")
        appendLine("Package: $packageName")
        if (appName.isNotEmpty()) appendLine("App: $appName")
        if (version.isNotEmpty()) appendLine("Version: $version")
        appendLine("Process: $processName")
        appendLine("UID: $uid · PID: $pid")
        appendLine("User: ${uid / 100_000}")
        metadata.reportFields().forEach { (label, value) -> appendLine("$label: $value") }
        appendLine(summary)
        appendLine()
        append(detail)
        if (truncated) append("\n\n[TRUNCATED]")
    }
}

object AppErrorRules {
    const val MAX_RECORDS = 200
    const val MAX_DETAIL = 48 * 1024
    const val MAX_SUMMARY = 512
    const val MAX_PENDING = 16
    private val packagePattern = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*")
    private val prefix = listOf(
        "java.lang.String", "com.android.server.am.ProcessRecord", "java.lang.String",
        "java.lang.String", "java.lang.String", "com.android.server.am.ProcessRecord",
        "java.lang.String", "java.lang.String", "java.io.File",
        "android.app.ApplicationErrorReport\$CrashInfo",
    )

    fun supportsSignature(types: List<String>): Boolean =
        types.size in 10..16 && types.take(prefix.size) == prefix

    fun kind(event: String?, exception: String?): AppErrorKind? = when (event) {
        "crash" -> if (exception == "Native crash") AppErrorKind.NATIVE else AppErrorKind.CRASH
        "native_crash" -> AppErrorKind.NATIVE
        "anr" -> AppErrorKind.ANR
        else -> null
    }

    fun canRead(caller: Int, owner: Int) = caller == owner
    fun canWrite(caller: Int) = caller == 1000

    fun bounded(record: AppErrorRecord, ownerUser: Int): AppErrorRecord? {
        if (record.time <= 0 || record.uid < 0 || record.uid / 100_000 != ownerUser || record.pid < 0) return null
        if (record.packageName.length !in 1..255 || !packagePattern.matches(record.packageName)) return null
        if (record.processName.length !in 1..255 || record.processName.any { it.isISOControl() }) return null
        return record.copy(
            summary = record.summary.take(MAX_SUMMARY).replace('\u0000', ' '),
            detail = record.detail.take(MAX_DETAIL).replace('\u0000', ' '),
            appName = record.appName.take(255).replace(Regex("[\\p{Cntrl}]"), " "),
            version = record.version.take(255).replace(Regex("[\\p{Cntrl}]"), " "),
            metadata = record.metadata.bounded(),
            truncated = record.truncated || record.detail.length > MAX_DETAIL || record.summary.length > MAX_SUMMARY || record.metadata != record.metadata.bounded(),
        )
    }

    fun combineDetails(parts: List<String?>): Pair<String, Boolean> {
        val result = StringBuilder()
        var truncated = false
        parts.filterNotNull().filter { it.isNotBlank() }.distinct().forEach { part ->
            if (result.isNotEmpty()) {
                if (result.length + 2 >= MAX_DETAIL) { truncated = true; return@forEach }
                result.append("\n\n")
            }
            val available = MAX_DETAIL - result.length
            result.append(part.take(available))
            if (part.length > available) truncated = true
        }
        return result.toString() to truncated
    }

    fun filter(records: List<AppErrorRecord>, kind: AppErrorKind?, query: String): List<AppErrorRecord> {
        val terms = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return records.filter { record ->
            (kind == null || record.kind == kind) && terms.all { term ->
                listOf(record.appName, record.packageName, record.processName, record.summary).any { it.contains(term, ignoreCase = true) }
            }
        }
    }
}
