package com.mi.onextbox.ui.screens

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.mi.onextbox.lsp.LspPreferenceStore
import com.mi.onextbox.lsp.OShareSettings
import com.mi.onextbox.ui.settings.FeatureSettingWriteQueue
import com.mi.onextbox.ui.settings.SettingsGroup
import com.mi.onextbox.ui.settings.SettingsToggleRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun OShareFeaturesPage() {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var enabled by remember(context) { mutableStateOf(OShareSettings.isEnabled(context)) }
    var applying by remember { mutableStateOf(false) }
    DisposableEffect(context, owner) {
        val prefs = LspPreferenceStore.prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (!applying && (key == null || key == OShareSettings.ENABLED))
                enabled = OShareSettings.isEnabled(context)
        }
        val observer = LifecycleEventObserver { _, event ->
            if (!applying && event == Lifecycle.Event.ON_RESUME) enabled = OShareSettings.isEnabled(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        owner.lifecycle.addObserver(observer)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            owner.lifecycle.removeObserver(observer)
        }
    }
    SettingsGroup {
        SettingsToggleRow(
            title = stringResource(R.string.oshare_unlimited_title),
            summary = stringResource(R.string.oshare_unlimited_summary),
            checked = enabled,
            onCheckedChange = { checked ->
                if (!applying) {
                    applying = true
                    enabled = checked
                    val pending = FeatureSettingWriteQueue.application.enqueue { OShareSettings.setEnabled(context, checked) }
                    scope.launch {
                        try {
                            pending.await()
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            Toast.makeText(context, R.string.oshare_save_failed, Toast.LENGTH_LONG).show()
                        } finally {
                            enabled = OShareSettings.isEnabled(context)
                            applying = false
                        }
                    }
                }
            },
        )
    }
}
