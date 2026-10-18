package com.mi.onextbox.refresh

import com.mi.onextbox.ui.common.AppLogStore
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CancellationException

/** Fallback work must not queue behind the app's shared LSP/touch shell. */
internal object RefreshRateCommandExecutor {
    data class Result(val isSuccess: Boolean, val out: List<String> = emptyList())
    private val runner = RefreshRateTaskRunner(1)

    suspend fun exec(tag: String, command: String): Result {
        return try {
            runner.run(9_000) {
                Shell.Builder.create().setTimeout(3).build().use { shell ->
                    if (!shell.isRoot) return@use Result(false)
                    // Bound the child too, so an old app_process does not apply later.
                    val result = shell.newJob().add("timeout -s KILL 5s env $command").exec()
                    AppLogStore.i("RefreshRate", "$tag: success=${result.isSuccess}")
                    result.err.take(3).forEach { AppLogStore.w("RefreshRate", it.take(240)) }
                    Result(result.isSuccess, result.out.toList())
                }
            } ?: Result(false).also { AppLogStore.w("RefreshRate", "$tag timed out") }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            AppLogStore.w("RefreshRate", "$tag failed: ${error.javaClass.simpleName}")
            Result(false)
        }
    }
}
