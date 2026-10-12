package com.mi.onextbox.ui.settings

import org.junit.Assert.*
import org.junit.Test

class AppUpdateRulesTest {
    @Test fun installedBuildTypeSelectsTheMatchingPackageIncludingOptimizedDebug() {
        assertEquals(UpdateBuildType.Release, AppUpdateRules.buildType("release"))
        assertEquals(UpdateBuildType.Debug, AppUpdateRules.buildType("debug"))
        assertEquals(UpdateBuildType.Debug, AppUpdateRules.buildType("debugSlim"))
        assertEquals(UpdateBuildType.Debug, AppUpdateRules.buildType("DEBUGSLIM"))
    }

    @Test fun releaseAndDebugAssetsNeverCrossBuildTypes() {
        val release = "onextbox-17.1-BETA1-EX01-release.apk"
        val debug = "onextbox-17.1-BETA1-EX01-debug.apk"
        assertTrue(AppUpdateRules.matchesApk(release, UpdateBuildType.Release))
        assertFalse(AppUpdateRules.matchesApk(release, UpdateBuildType.Debug))
        assertTrue(AppUpdateRules.matchesApk(debug, UpdateBuildType.Debug))
        assertFalse(AppUpdateRules.matchesApk(debug, UpdateBuildType.Release))
        assertTrue(AppUpdateRules.matchesApk(debug.uppercase(), UpdateBuildType.Debug))
        assertFalse(AppUpdateRules.matchesApk("debug.zip", UpdateBuildType.Debug))
        assertFalse(AppUpdateRules.matchesApk("SHA256SUMS", UpdateBuildType.Release))
    }

    @Test fun betaEnrollmentSwitchesToTheSeparateRepositoryOnly() {
        assertEquals("MiToverG422/ONextBox", AppUpdateRules.repository(false))
        assertEquals("MiToverG422/ONextBox_BetaTesting", AppUpdateRules.repository(true))
    }

    @Test fun mainReleasesExcludeBetaEvenIfNotMarkedAsPrerelease() {
        assertTrue(AppUpdateRules.acceptsRelease("v17.0", false, false, false))
        assertFalse(AppUpdateRules.acceptsRelease("v17.1-BETA1", false, false, false))
        assertFalse(AppUpdateRules.acceptsRelease("v17.1-beta.1", false, true, false))
        assertFalse(AppUpdateRules.acceptsRelease("v17.1", false, true, false))
        assertFalse(AppUpdateRules.acceptsRelease("17.0.0.47_Beta", false, false, false))
    }

    @Test fun betaRepositoryRequiresABetaMarkerButNotThePrereleaseCheckbox() {
        assertTrue(AppUpdateRules.acceptsRelease("v17.1-BETA1", false, true, true))
        assertTrue(AppUpdateRules.acceptsRelease("17.0.0.47_Beta", false, false, true))
        assertTrue(AppUpdateRules.acceptsRelease("17.0.0.47_BETA", false, true, true))
        assertFalse(AppUpdateRules.acceptsRelease("v17.1", false, false, true))
        assertFalse(AppUpdateRules.acceptsRelease("v17.1", false, true, true))
        assertFalse(AppUpdateRules.acceptsRelease("v17.1-BETA1", true, true, true))
        assertFalse(AppUpdateRules.acceptsRelease("ci-latest", false, true, true))
        assertFalse(AppUpdateRules.acceptsRelease("17.1-CI-deadbeef", false, true, true))
    }

    @Test fun betaMarkerCanComeFromTheReleaseTitleOrMatchingApk() {
        assertTrue(AppUpdateRules.acceptsRelease("v17.0.0.47", false, false, true,
            releaseName = "ONextBox 17.0.0.47_Beta"))
        assertTrue(AppUpdateRules.acceptsRelease("v17.0.0.47", false, false, true,
            assetName = "onextbox-17.0.0.47_Beta-release.apk"))
        assertFalse(AppUpdateRules.acceptsRelease("v17.0.0.47", false, false, false,
            assetName = "onextbox-17.0.0.47_Beta-release.apk"))
    }

    @Test fun releaseExtractionPreservesTheUnderscoreBetaMarkerAndAllFourNumbers() {
        assertEquals("17.0.0.47_Beta", AppUpdateRules.releaseVersionName(
            "v17.0.0.47_Beta", "ONextBox 17.0.0.47_Beta", "onextbox-17.0.0.47_Beta-release.apk", true))
        assertEquals("17.0.0.47_Beta", AppUpdateRules.releaseVersionName(
            "v17.0.0.47", "ONextBox 17.0.0.47_Beta", "onextbox-17.0.0.47_Beta-debug.apk", true))
        assertEquals("17.0.0.47_Beta", AppUpdateRules.releaseVersionName(
            "v17.0.0.47", "ONextBox", "onextbox-17.0.0.47_Beta-EX01-debug.apk", true))
        assertNull(AppUpdateRules.releaseVersionName("v17.0.0.47", "ONextBox", "onextbox-release.apk", true))
        assertEquals("17.0-CI-abcdef12", AppUpdateRules.releaseVersionName(
            "ci-latest", "ONextBox CI Latest 17.0-CI-abcdef12", "onextbox-debug.apk"))
    }

    @Test fun fourPartBetaVersionMustBeNumericallyHigherThanTheInstalledVersion() {
        assertTrue(AppUpdateRules.isNewerBetaVersion("17.0.0.47_Beta", "17.0.0.45"))
        assertTrue(AppUpdateRules.isNewerVersion("17.0.0.47_Beta", "17.0.0.45"))
        assertFalse(AppUpdateRules.isNewerBetaVersion("17.0.0.45_Beta", "17.0.0.45"))
        assertFalse(AppUpdateRules.isNewerBetaVersion("17.0.0.44_Beta", "17.0.0.45"))
        assertFalse(AppUpdateRules.isNewerBetaVersion("17.0.0.47", "17.0.0.45"))
        assertFalse(AppUpdateRules.isNewerBetaVersion("17.0.0.45_Beta", "17.0.0.45-CI-abcdef12"))
        assertFalse(AppUpdateRules.isNewerBetaVersion("17.0.0.45_Beta", "17.0.0.45_Beta"))
        assertFalse(AppUpdateRules.isNewerBetaVersion("17.0.0.45_Beta2", "17.0.0.45_Beta1"))
    }

    @Test fun betaVersionSegmentsUseNumericRatherThanAlphabeticalOrdering() {
        assertTrue(AppUpdateRules.isNewerBetaVersion("17.0.0.100_Beta", "17.0.0.99"))
        assertTrue(AppUpdateRules.isNewerBetaVersion("17.0.1.1_Beta", "17.0.0.999"))
        assertFalse(AppUpdateRules.isNewerBetaVersion("17.0.0.9_Beta", "17.0.0.45"))
        assertFalse(AppUpdateRules.isNewerBetaVersion("17.0.0.999_Beta", "17.0.1.1"))
        assertFalse(AppUpdateRules.isNewerBetaVersion("17.0_Beta", "17.0.0.0"))
        assertTrue(AppUpdateRules.isNewerBetaVersion("17.0.0.47_beta", "v17.0.0.45"))
    }

    @Test fun newerInstalledBetaCanReceiveAnotherHigherBetaOrAStableRelease() {
        assertTrue(AppUpdateRules.isNewerBetaVersion("17.0.0.48_Beta", "17.0.0.47_Beta"))
        assertFalse(AppUpdateRules.isNewerBetaVersion("17.0.0.46_Beta", "17.0.0.47_Beta"))
        assertTrue(AppUpdateRules.isNewerVersion("17.0.0.47", "17.0.0.47_Beta"))
        assertFalse(AppUpdateRules.isNewerVersion("17.0.0.47_Beta", "17.0.0.47"))
    }

    @Test fun underscoreBetaAssetsStillFollowTheInstalledBuildType() {
        val release = "onextbox-17.0.0.47_Beta-release.apk"
        val debug = "onextbox-17.0.0.47_Beta-debug.apk"
        assertTrue(AppUpdateRules.matchesApk(release, UpdateBuildType.Release))
        assertFalse(AppUpdateRules.matchesApk(release, UpdateBuildType.Debug))
        assertTrue(AppUpdateRules.matchesApk(debug, UpdateBuildType.Debug))
        assertFalse(AppUpdateRules.matchesApk(debug, UpdateBuildType.Release))
    }

    @Test fun stableVersionOutranksEveryBetaOfTheSameBaseVersion() {
        assertTrue(AppUpdateRules.isNewerVersion("17.0", "17.0-BETA99"))
        assertFalse(AppUpdateRules.isNewerVersion("17.0-BETA99", "17.0"))
        assertTrue(AppUpdateRules.compareVersions("17.0", "17.0-BETA99") > 0)
    }

    @Test fun betaVersionNumbersAreComparedNumerically() {
        assertTrue(AppUpdateRules.isNewerVersion("17.0-BETA10", "17.0-BETA9"))
        assertFalse(AppUpdateRules.isNewerVersion("17.0-BETA9", "17.0-BETA10"))
        assertTrue(AppUpdateRules.isNewerVersion("17.0-beta.2", "17.0-beta.1"))
        assertTrue(AppUpdateRules.isNewerVersion("17.0-beta-2", "17.0-beta-1"))
        assertEquals(0, AppUpdateRules.compareVersions("v17.0-BETA1", "17.0-beta.1"))
    }

    @Test fun betaPlanDoesNotDowngradeTheInstalledBaseVersion() {
        assertTrue(AppUpdateRules.isNewerVersion("17.1-BETA1", "17.0"))
        assertFalse(AppUpdateRules.isNewerVersion("17.0-BETA1", "17.1"))
        assertFalse(AppUpdateRules.isNewerVersion("17.0", "17.1-BETA1"))
        assertTrue(AppUpdateRules.isNewerVersion("17.1", "17.1-BETA1"))
    }

    @Test fun latestMatchingVersionIsIndependentOfReleaseArrayOrder() {
        val versions = listOf("17.0-BETA20", "17.0", "17.1-BETA1", "16.9")
        val comparator = Comparator<String>(AppUpdateRules::compareVersions)
        assertEquals("17.1-BETA1", versions.maxWithOrNull(comparator))
        assertEquals("17.1-BETA1", versions.reversed().maxWithOrNull(comparator))
        assertNull(emptyList<String>().maxWithOrNull(comparator))
    }

    @Test fun ciHashChangesRetainTheExistingLatestCiUpdateBehavior() {
        assertTrue(AppUpdateRules.isNewerVersion("17.0-CI-abcdef12", "17.0-CI-fedcba21"))
        assertFalse(AppUpdateRules.isNewerVersion("17.0-CI-abcdef12", "17.0-CI-abcdef12"))
        assertFalse(AppUpdateRules.isNewerVersion("17.0-CI-abcdef12", "17.0-BETA1"))
    }

    @Test fun malformedOrOversizedVersionsDoNotBecomeUpdates() {
        assertFalse(AppUpdateRules.isNewerVersion("", "17.0"))
        assertFalse(AppUpdateRules.isNewerVersion("not-a-version", "17.0"))
        assertFalse(AppUpdateRules.isNewerVersion("17.999999999999999999", "17.0"))
        assertFalse(AppUpdateRules.isNewerVersion("17.0", "17.0.0"))
    }
}
