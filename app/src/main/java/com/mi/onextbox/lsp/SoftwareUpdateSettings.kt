package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal enum class SoftwareUpdateFeature(val key: String) {
    HideRoot("ota_hide_root"),
    HideBootloader("ota_hide_bootloader"),
    ShowLocalInstall("ota_show_local_install"),
    BlockSystemUpdate("ota_block_system_update"),
    BlockSystemFiles("ota_block_system_files"),
    DisableAutoDownload("ota_disable_auto_download"),
    DisableNightInstall("ota_disable_night_install"),
    HideRecommendations("ota_hide_recommendations"),
    HideUpdatePopup("ota_hide_update_popup"),
    PackageDetails("ota_package_details"),
}

internal object SoftwareUpdateSettings {
    const val PACKAGE_NAME = "com.oplus.ota"
    const val SYSTEM_FILES_PACKAGE_NAME = "com.oplus.sau"

    fun enabled(context: Context, feature: SoftwareUpdateFeature): Boolean =
        LspPreferenceStore.prefs(context).getBoolean(feature.key, false)

    fun enabledXposed(feature: SoftwareUpdateFeature): Boolean =
        HookConfigSnapshot.boolean(feature.key, false) == true

    fun setEnabled(context: Context, feature: SoftwareUpdateFeature, enabled: Boolean) {
        ModernXposedPreferenceSync.initialize(context)
        check(LspPreferenceStore.prefs(context).edit().putBoolean(feature.key, enabled).commit())
    }
}
