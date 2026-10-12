package com.mi.onextbox.ui.settings

import java.util.Locale

internal object AppUpdateRules {
    const val MAIN_REPOSITORY = "MiToverG422/ONextBox"
    const val BETA_REPOSITORY = "MiToverG422/ONextBox_BetaTesting"

    fun repository(betaPlanEnabled: Boolean): String = if (betaPlanEnabled) BETA_REPOSITORY else MAIN_REPOSITORY

    val versionNameRegex = Regex(
        """\d+(?:\.\d+)+(?:[-_]BETA(?:[.-]?\d+(?:\.\d+)*)?|-CI-[A-Za-z0-9]+)?""",
        RegexOption.IGNORE_CASE,
    )

    fun buildType(installedBuildType: String): UpdateBuildType =
        if (installedBuildType.startsWith("debug", ignoreCase = true)) UpdateBuildType.Debug else UpdateBuildType.Release

    fun matchesApk(name: String, buildType: UpdateBuildType): Boolean {
        val normalized = name.lowercase(Locale.ROOT)
        return normalized.endsWith(".apk") && when (buildType) {
            UpdateBuildType.Release -> !normalized.contains("debug")
            UpdateBuildType.Debug -> normalized.contains("debug")
        }
    }

    fun acceptsRelease(
        tag: String,
        draft: Boolean,
        prerelease: Boolean,
        includeBeta: Boolean,
        releaseName: String = "",
        assetName: String = "",
    ): Boolean {
        if (draft || tag.equals("ci-latest", ignoreCase = true) || tag.contains("-CI-", ignoreCase = true)) return false
        val beta = listOf(tag, releaseName, assetName).any(::isBetaVersion)
        return if (includeBeta) beta else !prerelease && !beta
    }

    fun isBetaVersion(version: String): Boolean =
        version.contains("_Beta", ignoreCase = true) || version.contains("-BETA", ignoreCase = true)

    fun releaseVersionName(tag: String, releaseName: String, assetName: String, requireBeta: Boolean = false): String? {
        val versions = listOf(tag, releaseName, assetName).mapNotNull { versionNameRegex.find(it)?.value }
        return versions.firstOrNull { !requireBeta || isBetaVersion(it) }
    }

    fun isNewerBetaVersion(remote: String, current: String): Boolean {
        if (!isBetaVersion(remote)) return false
        val a = parse(remote) ?: return false
        val b = parse(current) ?: return false
        return compareParts(a.parts, b.parts) > 0
    }

    fun compareVersions(first: String, second: String): Int {
        val a = parse(first) ?: return if (parse(second) == null) 0 else -1
        val b = parse(second) ?: return 1
        compareParts(a.parts, b.parts).takeIf { it != 0 }?.let { return it }
        a.rank.compareTo(b.rank).takeIf { it != 0 }?.let { return it }
        return compareParts(a.qualifierParts, b.qualifierParts)
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank() || remote.equals(current, ignoreCase = true) || parse(remote) == null) return false
        val comparison = compareVersions(remote, current)
        if (comparison != 0) return comparison > 0
        val a = parse(remote) ?: return false
        val b = parse(current) ?: return true
        return a.rank == 1 && b.rank == 1 && a.qualifier != b.qualifier
    }

    private data class Version(val parts: List<Int>, val rank: Int, val qualifierParts: List<Int>, val qualifier: String)

    private fun parse(version: String): Version? {
        val comparable = versionNameRegex.find(version.trim().removePrefix("v").removePrefix("V"))?.value ?: return null
        val separator = comparable.indexOfFirst { it == '-' || it == '_' }
        val qualifier = if (separator >= 0) comparable.substring(separator + 1).uppercase(Locale.ROOT) else ""
        val rank = when {
            qualifier.startsWith("CI-") -> 1
            qualifier.startsWith("BETA") -> 2
            qualifier.isEmpty() -> 3
            else -> 0
        }
        val qualifierParts = if (rank == 2) qualifier.removePrefix("BETA").trimStart('.', '-')
            .split('.').mapNotNull(String::toIntOrNull) else emptyList()
        val parts = comparable.take(if (separator >= 0) separator else comparable.length)
            .split('.').map { it.toIntOrNull() ?: return null }
        return Version(parts, rank, qualifierParts, qualifier)
    }

    private fun compareParts(first: List<Int>, second: List<Int>): Int {
        for (index in 0 until maxOf(first.size, second.size)) {
            first.getOrElse(index) { 0 }.compareTo(second.getOrElse(index) { 0 })
                .takeIf { it != 0 }?.let { return it }
        }
        return 0
    }
}
