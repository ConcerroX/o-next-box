package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal object FluidCloudMediaSettings {
    const val ENABLED = "systemui_fluid_cloud_media_blacklist_enabled"
    const val PACKAGES = "systemui_fluid_cloud_media_blacklist_packages"

    fun isEnabled(context: Context): Boolean = LspPreferenceStore.prefs(context).getBoolean(ENABLED, false)

    fun packages(context: Context): Set<String> = FluidCloudMediaRules.normalize(
        LspPreferenceStore.prefs(context).getStringSet(PACKAGES, emptySet()).orEmpty(),
    )

    fun setEnabled(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(ENABLED, enabled).commit())
        LspConfig.syncReadableState(context)
    }

    fun setPackages(context: Context, packages: Set<String>) {
        check(LspPreferenceStore.prefs(context).edit()
            .putStringSet(PACKAGES, FluidCloudMediaRules.normalize(packages)).commit())
        LspConfig.syncReadableState(context)
    }

    fun isEnabledXposed(): Boolean = HookConfigSnapshot.boolean(ENABLED, false) == true

    fun blockedXposed(): Set<String> = if (isEnabledXposed()) {
        HookConfigSnapshot.stringSet(PACKAGES, emptySet()).orEmpty()
    } else emptySet()
}
