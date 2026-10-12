package com.mi.onextbox.ui.screens

internal class FeatureRestartPlan(packages: List<String>) {
    val appPackages: List<String> = packages
        .filter { it.isNotBlank() && it != "android" && it != "system" }
        .distinct()
    val includesFramework: Boolean = packages.any { it == "android" || it == "system" }
    val requiresDeviceReboot: Boolean = includesFramework && appPackages.isEmpty()

    fun shouldRebootDevice(requested: Boolean): Boolean = requiresDeviceReboot || requested
}
