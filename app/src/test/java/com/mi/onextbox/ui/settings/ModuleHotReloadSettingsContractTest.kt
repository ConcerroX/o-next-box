package com.mi.onextbox.ui.settings

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleHotReloadSettingsContractTest {
    @Test fun standaloneGroupFollowsDisplayInSharedSettingsPage() {
        val page = mainFile("java/com/mi/onextbox/ui/settings/SettingsMainPage.kt")
            .readText().substringAfter("fun AppSettingsPage(")
            .substringBefore("private fun ModuleHotReloadSettingsGroup()")
        val display = page.indexOf("R.string.settings_section_display")
        val hotReload = page.indexOf("ModuleHotReloadSettingsGroup()")
        val backup = page.indexOf("R.string.section_restore_backup")
        assertTrue(display >= 0)
        assertTrue(hotReload > display)
        assertTrue(backup > hotReload)
        assertEquals(1, Regex("ModuleHotReloadSettingsGroup\\(\\)").findAll(page).count())
    }

    @Test fun preferenceDefaultsOffAndUsesFrameworkSyncWithoutRoot() {
        val source = mainFile("java/com/mi/onextbox/lsp/ModuleHotReloadSettings.kt").readText()
        assertTrue(source.contains("getBoolean(PREFERENCE_KEY, false)"))
        assertTrue(source.contains("ModernXposedPreferenceSync.setBooleanAndSync(context, PREFERENCE_KEY, enabled)"))
        assertFalse(source.contains("RootShell"))
        assertFalse(source.contains("Shell.cmd"))
    }

    @Test fun everyAppLocaleIncludesHotReloadStrings() {
        val keys = listOf(
            "settings_section_module_hot_reload",
            "setting_module_hot_reload",
            "setting_module_hot_reload_summary",
            "setting_module_hot_reload_save_failed",
        )
        val locales = listOf(
            "values", "values-zh-rCN", "values-zh-rTW", "values-b+yue+Hant",
            "values-ja", "values-ja-rCN", "values-ko", "values-ru", "values-vi",
        )
        for (locale in locales) {
            val source = mainFile("res/$locale/strings.xml").readText()
            for (key in keys) {
                assertEquals("$locale: $key", 1,
                    Regex("name=\"$key\"").findAll(source).count())
            }
        }
    }

    private fun mainFile(path: String): File = listOf(
        File("src/main/$path"),
        File("app/src/main/$path"),
    ).first { it.isFile }
}
