package com.mi.onextbox.ui.screens

import java.text.Collator
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaBlacklistListModelTest {
    private val apps = listOf(
        MediaBlacklistApp("com.test.zebra", "Zebra"),
        MediaBlacklistApp("com.test.beta", "Beta"),
        MediaBlacklistApp("com.test.alpha", "Alpha"),
    )

    @Test fun selectedAppsComeFirstWithAlphabeticalOrderInBothGroups() {
        val listed = mediaBlacklistApps(apps, setOf("com.test.zebra", "com.test.beta"), "", Locale.US)
        assertEquals(listOf("Beta", "Zebra", "Alpha"), listed.map { it.label })
    }

    @Test fun noSelectionAndFullSelectionBothUseNameOrder() {
        val expected = listOf("Alpha", "Beta", "Zebra")
        assertEquals(expected, mediaBlacklistApps(apps, emptySet(), "", Locale.US).map { it.label })
        assertEquals(expected, mediaBlacklistApps(apps, apps.map { it.packageName }.toSet(), "", Locale.US).map { it.label })
    }

    @Test fun changingSelectionReordersWithoutRemovingInstalledApps() {
        val selected = setOf("com.test.zebra")
        assertEquals(listOf("Zebra", "Alpha", "Beta"), mediaBlacklistApps(apps, selected, "", Locale.US).map { it.label })
        assertEquals(listOf("Alpha", "Beta", "Zebra"), mediaBlacklistApps(apps, emptySet(), "", Locale.US).map { it.label })
        assertEquals(listOf("Zebra", "Beta", "Alpha"), apps.map { it.label })
    }

    @Test fun selectedUnavailablePackagesRemainVisibleAndCanBeRemoved() {
        val selected = linkedSetOf("com.test.missing", "com.test.alpha")
        val listed = mediaBlacklistApps(apps, selected, "", Locale.US)
        assertEquals(listOf("com.test.alpha", "com.test.missing", "com.test.beta", "com.test.zebra"), listed.map { it.packageName })
        assertEquals("com.test.missing", listed[1].label)
        assertEquals(1, listed.count { it.packageName == "com.test.alpha" })
        assertEquals(3, mediaBlacklistApps(apps, selected - "com.test.missing", "", Locale.US).size)
    }

    @Test fun searchingNamesAndPackagesIsCaseInsensitiveAndKeepsSelectedPriority() {
        assertEquals(listOf("Beta"), mediaBlacklistApps(apps, emptySet(), "  bETA  ", Locale.US).map { it.label })
        assertEquals(listOf("Zebra", "Alpha", "Beta"), mediaBlacklistApps(apps, setOf("com.test.zebra"), "COM.TEST", Locale.US).map { it.label })
        assertEquals(listOf("com.test.missing"), mediaBlacklistApps(apps, setOf("com.test.missing"), "MISSING", Locale.US).map { it.packageName })
        assertTrue(mediaBlacklistApps(apps, setOf("com.test.zebra"), "no match", Locale.US).isEmpty())
    }

    @Test fun identicalLabelsUsePackageNamesForDeterministicOrder() {
        val sameLabels = listOf(MediaBlacklistApp("com.test.b", "Player"), MediaBlacklistApp("com.test.a", "Player"))
        assertEquals(listOf("com.test.a", "com.test.b"), mediaBlacklistApps(sameLabels, emptySet(), "", Locale.US).map { it.packageName })
        assertEquals(listOf("com.test.b", "com.test.a"), mediaBlacklistApps(sameLabels, setOf("com.test.b"), "", Locale.US).map { it.packageName })
    }

    @Test fun labelsUseTheConfiguredLocaleRatherThanRawUnicodeOrder() {
        val localized = listOf(MediaBlacklistApp("com.test.z", "Zulu"), MediaBlacklistApp("com.test.a", "Äther"))
        val locale = Locale.GERMAN
        val collator = Collator.getInstance(locale)
        assertTrue(collator.compare("Äther", "Zulu") < 0)
        assertEquals(listOf("Äther", "Zulu"), mediaBlacklistApps(localized, emptySet(), "", locale).map { it.label })
    }

    @Test fun switchingLocaleReordersTheSameAppsWithoutChangingSelection() {
        val localized = listOf(MediaBlacklistApp("com.test.z", "Zulu"), MediaBlacklistApp("com.test.a", "Äther"))
        val selected = localized.map { it.packageName }.toSet()
        assertEquals(listOf("Äther", "Zulu"),
            mediaBlacklistApps(localized, selected, "", Locale.GERMAN).map { it.label })
        assertEquals(listOf("Zulu", "Äther"),
            mediaBlacklistApps(localized, selected, "", Locale.forLanguageTag("sv")).map { it.label })
        assertEquals(selected, mediaBlacklistApps(localized, selected, "", Locale.GERMAN).map { it.packageName }.toSet())
    }

    @Test fun emptyAndSingleRowResultsRemainValid() {
        assertTrue(mediaBlacklistApps(emptyList(), emptySet(), "", Locale.US).isEmpty())
        assertEquals(listOf(MediaBlacklistApp("com.test.missing", "com.test.missing")),
            mediaBlacklistApps(emptyList(), setOf("com.test.missing"), "", Locale.US))
        assertEquals(listOf(apps[0]), mediaBlacklistApps(apps, emptySet(), "Zebra", Locale.US))
    }
}
