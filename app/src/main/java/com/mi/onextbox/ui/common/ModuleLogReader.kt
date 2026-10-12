package com.mi.onextbox.ui.common

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CancellationException

internal enum class ModuleLogFailure { ROOT_REQUIRED, UNAVAILABLE, READ_FAILED }

internal data class ModuleLogReadResult(
    val records: List<LogRecord> = emptyList(),
    val failure: ModuleLogFailure? = null,
)

/** Read-only, bounded LSPosed log access; does not change framework logging settings. */
internal object ModuleLogReader {
    private const val MISSING = "__ONEXTBOX_LOG_MISSING__"
    private val command = """
        onextbox_latest=''
        for onextbox_file in /data/adb/lspd/log/modules*.log; do
            [ -f "${'$'}onextbox_file" ] || continue
            if [ -z "${'$'}onextbox_latest" ] || [ "${'$'}onextbox_file" -nt "${'$'}onextbox_latest" ]; then
                onextbox_latest="${'$'}onextbox_file"
            fi
        done
        if [ -z "${'$'}onextbox_latest" ]; then
            printf '$MISSING\n'
        else
            tail -c 1048576 "${'$'}onextbox_latest"
        fi
    """.trimIndent()

    fun read(): ModuleLogReadResult {
        val shell = runCatching { Shell.getCachedShell()?.takeIf { it.isRoot && it.isAlive } }.getOrNull()
            ?: return ModuleLogReadResult(failure = ModuleLogFailure.ROOT_REQUIRED)
        return try {
            val result = shell.newJob().add(command).exec()
            when {
                !result.isSuccess -> ModuleLogReadResult(failure = ModuleLogFailure.READ_FAILED)
                result.out.singleOrNull() == MISSING -> ModuleLogReadResult(failure = ModuleLogFailure.UNAVAILABLE)
                else -> ModuleLogReadResult(records = ModuleLogParser.parse(result.out))
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            ModuleLogReadResult(failure = ModuleLogFailure.READ_FAILED)
        }
    }
}
