package com.mi.onextbox.ui.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mi.onextbox.R
import com.mi.onextbox.lsp.LauncherGridRules
import com.mi.onextbox.lsp.LauncherGridSettings
import com.mi.onextbox.lsp.LauncherGridState
import com.mi.onextbox.ui.common.AppLogStore
import com.mi.onextbox.ui.settings.FeatureSettingWriteQueue
import com.mi.onextbox.ui.settings.SettingsDivider
import com.mi.onextbox.ui.settings.SettingsToggleRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun LauncherGridSettingsGroup() {
    val context = LocalContext.current
    val application = context.applicationContext
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var state by remember(context) { mutableStateOf(LauncherGridSettings.state(context)) }
    var pending by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    fun save(next: LauncherGridState) {
        state = next
        val token = ++revision
        pending++
        val write = FeatureSettingWriteQueue.application.enqueue { LauncherGridSettings.save(application, next) }
        scope.launch {
            try {
                write.await()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (revision == token) state = LauncherGridSettings.state(application)
                AppLogStore.w("LauncherGrid", "Unable to save custom grid: ${error.message.orEmpty()}")
                Toast.makeText(context, R.string.launcher_custom_grid_save_failed, Toast.LENGTH_SHORT).show()
            } finally {
                pending--
            }
        }
    }
    DisposableEffect(context, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pending == 0) state = LauncherGridSettings.state(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    androidx.compose.runtime.key("launcher-custom-grid") {
        SettingsToggleRow(
            title = stringResource(R.string.launcher_custom_grid_title),
            summary = "",
            checked = state.enabled,
            onCheckedChange = { save(state.copy(enabled = it)) },
            hasDividerAbove = true,
            hasDividerBelow = true,
        )
        SettingsDivider()
        FeatureSliderRow(
            heading = stringResource(R.string.launcher_custom_grid_columns),
            currentValue = stringResource(R.string.launcher_custom_grid_columns_value, state.grid.columns),
            value = state.grid.columns.toFloat(),
            valueRange = LauncherGridRules.MIN_COLUMNS.toFloat()..LauncherGridRules.MAX_COLUMNS.toFloat(),
            steps = LauncherGridRules.MAX_COLUMNS - LauncherGridRules.MIN_COLUMNS - 1,
            enabled = state.enabled,
            onValueChange = { state = state.copy(grid = state.grid.copy(columns = it.roundToInt())) },
            onValueChangeFinished = { save(state) },
            onReset = { save(state.copy(grid = state.grid.copy(columns = 4))) },
            hasDividerBelow = true,
        )
        SettingsDivider()
        FeatureSliderRow(
            heading = stringResource(R.string.launcher_custom_grid_rows),
            currentValue = stringResource(R.string.launcher_custom_grid_rows_value, state.grid.rows),
            value = state.grid.rows.toFloat(),
            valueRange = LauncherGridRules.MIN_ROWS.toFloat()..LauncherGridRules.MAX_ROWS.toFloat(),
            steps = LauncherGridRules.MAX_ROWS - LauncherGridRules.MIN_ROWS - 1,
            enabled = state.enabled,
            onValueChange = { state = state.copy(grid = state.grid.copy(rows = it.roundToInt())) },
            onValueChangeFinished = { save(state) },
            onReset = { save(state.copy(grid = state.grid.copy(rows = 7))) },
            hasDividerBelow = false,
        )
    }
}
