package com.mi.onextbox.ui.screens

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mi.onextbox.R
import com.mi.onextbox.lsp.AospVolumeSettings
import com.mi.onextbox.ui.settings.SettingsToggleRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AospVolumeSettingsRow() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(AospVolumeSettings.enabled(context)) }
    var saving by remember { mutableStateOf(false) }
    DisposableEffect(context, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = AospVolumeSettings.enabled(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    SettingsToggleRow(title = stringResource(R.string.feature_aosp_volume_title),
        summary = stringResource(R.string.feature_aosp_volume_summary), checked = enabled,
        hasDividerAbove = true, hasDividerBelow = true,
        onCheckedChange = { checked ->
            if (!saving) {
                saving = true
                enabled = checked
                scope.launch {
                    try { withContext(Dispatchers.IO) { AospVolumeSettings.set(context, checked) } }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { Toast.makeText(context, R.string.feature_aosp_volume_save_failed, Toast.LENGTH_LONG).show() }
                    finally { enabled = AospVolumeSettings.enabled(context); saving = false }
                }
            }
        })
}
