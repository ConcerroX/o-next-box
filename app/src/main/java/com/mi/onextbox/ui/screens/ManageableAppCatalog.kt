package com.mi.onextbox.ui.screens

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/** Management lists must not depend on whether an app can currently be launched. */
internal fun manageableApplications(pm: PackageManager): List<ApplicationInfo> {
    val flags = PackageManager.MATCH_DISABLED_COMPONENTS or
        PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS or PackageManager.MATCH_UNINSTALLED_PACKAGES
    val launcherPackages = pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), flags,
    ).mapNotNull { it.activityInfo?.packageName }.toSet()
    return pm.getInstalledApplications(flags).filter { info ->
        includeInManagementCatalog(
            installed = info.flags and ApplicationInfo.FLAG_INSTALLED != 0,
            system = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            hasLauncher = info.packageName in launcherPackages,
        )
    }.distinctBy { it.packageName }
}

internal fun includeInManagementCatalog(installed: Boolean, system: Boolean, hasLauncher: Boolean): Boolean =
    installed && (!system || hasLauncher)
