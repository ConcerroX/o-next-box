package com.mi.onextbox.ui.screens

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
import com.mi.onextbox.lsp.LspConfig
import com.mi.onextbox.lsp.FluidCloudBrowserSettings
import com.mi.onextbox.lsp.FluidCloudMediaSettings
import com.mi.onextbox.lsp.LspPreferenceStore
import com.mi.onextbox.ui.settings.FeatureSettingWriteQueue
import com.mi.onextbox.ui.settings.SettingsCardRow
import com.mi.onextbox.ui.settings.SettingsDivider
import com.mi.onextbox.ui.settings.SettingsGroup
import com.mi.onextbox.ui.settings.SettingsSection
import com.mi.onextbox.ui.settings.SettingsToggleRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun FluidCloudFeaturesPage(onOpen: (FeaturePageMode) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var enabled by remember(context) { mutableStateOf(LspConfig.isFluidCloudMaterialEnabled(context)) }
    var applying by remember { mutableStateOf(false) }
    var mediaEnabled by remember(context) { mutableStateOf(FluidCloudMediaSettings.isEnabled(context)) }
    var mediaCount by remember(context) { mutableStateOf(FluidCloudMediaSettings.packages(context).size) }
    var mediaApplying by remember { mutableStateOf(false) }
    var browserEnabled by remember(context) { mutableStateOf(FluidCloudBrowserSettings.isEnabled(context)) }
    var browserApplying by remember { mutableStateOf(false) }
    DisposableEffect(context, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !applying) {
                enabled = LspConfig.isFluidCloudMaterialEnabled(context)
                mediaEnabled = FluidCloudMediaSettings.isEnabled(context)
                mediaCount = FluidCloudMediaSettings.packages(context).size
            }
            if (event == Lifecycle.Event.ON_RESUME && !browserApplying) {
                browserEnabled = FluidCloudBrowserSettings.isEnabled(context)
            }
        }
        owner.lifecycle.addObserver(observer)
        val prefs = LspPreferenceStore.prefs(context)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == FluidCloudMediaSettings.ENABLED || key == FluidCloudMediaSettings.PACKAGES) {
                mediaEnabled = FluidCloudMediaSettings.isEnabled(context)
                mediaCount = FluidCloudMediaSettings.packages(context).size
            }
            if (!browserApplying && (key == null || key == FluidCloudBrowserSettings.ENABLED)) {
                browserEnabled = FluidCloudBrowserSettings.isEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        if (!browserApplying) browserEnabled = FluidCloudBrowserSettings.isEnabled(context)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    SettingsSection(title = stringResource(R.string.feature_fluid_cloud_card_group))
    SettingsGroup {
        SettingsToggleRow(
            title = stringResource(R.string.feature_fluid_cloud_material_title),
            summary = "",
            checked = enabled,
            enabled = !applying,
            onCheckedChange = { checked ->
                applying = true
                enabled = checked
                scope.launch {
                    try {
                        val success = withContext(Dispatchers.IO) {
                            LspConfig.setFluidCloudMaterialEnabled(context, checked)
                            LspConfig.isFluidCloudMaterialEnabled(context) == checked
                        }
                        enabled = LspConfig.isFluidCloudMaterialEnabled(context)
                        if (!success) {
                            Toast.makeText(context, R.string.feature_fluid_cloud_material_failed, Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        applying = false
                    }
                }
            },
        )
    }
    SettingsSection(title = stringResource(R.string.fluid_media_group))
    SettingsGroup {
        SettingsToggleRow(
            title = stringResource(R.string.fluid_media_blacklist_title),
            summary = stringResource(R.string.fluid_media_blacklist_summary),
            checked = mediaEnabled,
            enabled = !mediaApplying,
            hasDividerBelow = true,
            onCheckedChange = { checked ->
                mediaApplying = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { FluidCloudMediaSettings.setEnabled(context, checked) }
                        mediaEnabled = FluidCloudMediaSettings.isEnabled(context)
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) {
                        Toast.makeText(context, R.string.fluid_media_save_failed, Toast.LENGTH_LONG).show()
                    } finally { mediaApplying = false }
                }
            },
        )
        SettingsDivider()
        SettingsCardRow(
            title = stringResource(R.string.fluid_media_apps_title),
            summary = stringResource(R.string.fluid_media_selected_count, mediaCount),
            hasDividerAbove = true,
            showArrow = true,
            onClick = { onOpen(FeaturePageMode.SystemUiFluidCloudMediaBlacklist) },
        )
    }
    SettingsSection(title = stringResource(R.string.fluid_browser_group))
    SettingsGroup {
        SettingsToggleRow(
            title = stringResource(R.string.fluid_browser_title),
            summary = stringResource(R.string.fluid_browser_summary),
            checked = browserEnabled,
            enabled = !browserApplying,
            onCheckedChange = { checked ->
                if (!browserApplying) {
                    browserApplying = true
                    browserEnabled = checked
                    val appContext = context.applicationContext
                    val pending = FeatureSettingWriteQueue.application.enqueue {
                        FluidCloudBrowserSettings.setEnabled(appContext, checked)
                        check(FluidCloudBrowserSettings.isEnabled(appContext) == checked)
                    }
                    scope.launch {
                        try {
                            pending.await()
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            Toast.makeText(appContext, R.string.fluid_browser_save_failed, Toast.LENGTH_LONG).show()
                        } finally {
                            browserEnabled = FluidCloudBrowserSettings.isEnabled(appContext)
                            browserApplying = false
                        }
                    }
                }
            },
        )
    }
}
