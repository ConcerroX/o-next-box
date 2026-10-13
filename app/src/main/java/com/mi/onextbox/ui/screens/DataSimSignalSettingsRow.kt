package com.mi.onextbox.ui.screens

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mi.onextbox.R
import com.mi.onextbox.lsp.DataSimSignalSettings
import com.mi.onextbox.ui.settings.FeatureSettingWriteQueue
import com.mi.onextbox.ui.settings.SettingsToggleRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun DataSimSignalSettingsRow() {
    val context = LocalContext.current
    val application = context.applicationContext
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(DataSimSignalSettings.enabled(context)) }
    var saving by remember { mutableStateOf(false) }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !saving) enabled = DataSimSignalSettings.enabled(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    SettingsToggleRow(
        title = stringResource(R.string.feature_only_data_sim_signal_title),
        summary = stringResource(R.string.feature_only_data_sim_signal_summary),
        checked = enabled,
        switchEnabled = !saving,
        hasDividerBelow = true,
        onCheckedChange = { next ->
            if (!saving) {
                enabled = next
                saving = true
                val write = FeatureSettingWriteQueue.application.enqueue { DataSimSignalSettings.set(application, next) }
                scope.launch {
                    try { write.await() }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) {
                        Toast.makeText(context, R.string.enabled_features_save_failed, Toast.LENGTH_LONG).show()
                    } finally {
                        enabled = DataSimSignalSettings.enabled(application)
                        saving = false
                    }
                }
            }
        },
    )
}
