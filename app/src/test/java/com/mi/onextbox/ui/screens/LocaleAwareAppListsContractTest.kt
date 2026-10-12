package com.mi.onextbox.ui.screens

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleAwareAppListsContractTest {
    private val pages = listOf("FluidCloudMediaBlacklistPage", "NotificationBatchPage", "SideKeyFeaturesPage")

    @Test fun allAppListsObserveComposeLocaleConfiguration() {
        pages.forEach { name ->
            val source = source(name)
            assertTrue(name, source.contains("val locale = LocalConfiguration.current.locales[0]"))
            assertFalse(name, source.contains("context.resources.configuration"))
        }
    }

    @Test fun changingLocaleReloadsEachAppCatalog() {
        pages.forEach { name ->
            assertTrue(name, source(name).contains("LaunchedEffect(context, locale"))
        }
    }

    @Test fun localeChangesInvalidateSortingAndBackgroundSortsUseTheCapturedLocale() {
        val blacklist = source("FluidCloudMediaBlacklistPage")
        assertTrue(blacklist.contains("remember(apps, selected, query, locale)"))
        assertTrue(blacklist.contains("mediaBlacklistApps(apps, selected, query, locale)"))
        listOf("NotificationBatchPage", "SideKeyFeaturesPage").forEach { name ->
            assertTrue(name, source(name).contains("Collator.getInstance(locale)"))
        }
    }

    private fun source(name: String): String = listOf(
        File("src/main/java/com/mi/onextbox/ui/screens/$name.kt"),
        File("app/src/main/java/com/mi/onextbox/ui/screens/$name.kt"),
    ).first { it.isFile }.readText()
}
