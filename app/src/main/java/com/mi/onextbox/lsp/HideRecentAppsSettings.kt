package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal object HideRecentAppsSettings {
    const val ENABLED = "launcher_hide_recent_apps_enabled"
    const val PACKAGES = "launcher_hide_recent_apps_packages"
    fun enabled(context: Context) = LspPreferenceStore.prefs(context).getBoolean(ENABLED, false)
    fun packages(context: Context): Set<String> = FluidCloudMediaRules.normalize(
        LspPreferenceStore.prefs(context).getStringSet(PACKAGES, emptySet()).orEmpty())
    fun setEnabled(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(ENABLED, enabled).commit())
        LspConfig.syncReadableState(context)
    }
    fun setPackages(context: Context, packages: Set<String>) {
        check(LspPreferenceStore.prefs(context).edit()
            .putStringSet(PACKAGES, FluidCloudMediaRules.normalize(packages)).commit())
        LspConfig.syncReadableState(context)
    }
    fun hiddenXposed(): Set<String> = if (HookConfigSnapshot.boolean(ENABLED, false) == true)
        HookConfigSnapshot.stringSet(PACKAGES, emptySet()).orEmpty() else emptySet()
}

internal object HideRecentAppsRules {
    fun hideGroup(packages: List<String?>, hidden: Set<String>): Boolean =
        packages.any { !it.isNullOrEmpty() && it in hidden }
}
