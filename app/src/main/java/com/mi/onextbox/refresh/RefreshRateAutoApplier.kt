package com.mi.onextbox.refresh

import android.content.Context
import com.mi.onextbox.ui.common.AppLogStore

object RefreshRateAutoApplier {
    suspend fun applyIfEnabled(context: Context): Boolean {
        if (!RefreshRatePreferences.readAutoStartEnabled(context)) return true
        val mode = RefreshRatePreferences.readSelectedMode(context) ?: run {
            AppLogStore.w("RefreshRate", "Auto apply skipped: no saved display mode")
            return false
        }
        val lines = RefreshRateControllerClient.getSupportedModes(context).orEmpty().ifEmpty {
            RefreshRateCommandExecutor.exec("Boot mode discovery", command(context, "list")).out
        }
        val current = RefreshRateModeResolver.resolve(mode, lines) ?: run {
            AppLogStore.w("RefreshRate", "Auto apply deferred: saved resolution/rate unavailable")
            return false
        }
        // A selection made after this job started takes precedence over boot restore.
        if (!RefreshRatePreferences.readAutoStartEnabled(context) || RefreshRatePreferences.readSelectedMode(context) != mode) return true
        val success = RefreshRateControllerClient.setRefreshRateMode(context, current.surfaceFlingerModeIndex)
            ?: RefreshRateCommandExecutor.exec("Boot mode apply", command(context, "set ${current.surfaceFlingerModeIndex}")).isSuccess
        if (success) {
            AppLogStore.i(
                "RefreshRate",
                "Auto apply accepted config=${current.surfaceFlingerModeIndex} " +
                    "${current.width}x${current.height}@${current.refreshRate}"
            )
        } else {
            AppLogStore.w("RefreshRate", "Auto apply failed for config=${mode.surfaceFlingerModeIndex}")
        }
        return success
    }

    private fun command(context: Context, args: String): String {
        val path = "'" + context.applicationInfo.sourceDir.replace("'", "'\"'\"'") + "'"
        return "CLASSPATH=$path app_process /system/bin com.mi.onextbox.tools.SurfaceFlingerModeTool $args"
    }
}
