package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ModuleStringLookupTest {
    @Test fun changedIdsDoNotSelectUnrelatedStrings() {
        val name = "settings_app_info_package"
        val old = mapOf(10 to "Package name")
        val updated = mapOf(10 to "Module hot reload", 42 to "Package name")
        assertEquals("Package name", ModuleStringLookup.resolve(name, "—", { 10 }, { old.getValue(it) }))
        assertEquals("Package name", ModuleStringLookup.resolve(name, "—", { 42 }, { updated.getValue(it) }))
        assertEquals("Package name", ModuleStringLookup.resolve(name, "—", { 10 }, { old.getValue(it) }))
    }
    @Test fun missingOrBrokenResourcesFailClosed() {
        assertEquals("—", ModuleStringLookup.resolve("missing", "—", { 0 }, { error("Must not read ID zero") }))
        assertEquals("Unavailable", ModuleStringLookup.resolve("missing", "Unavailable", { 2 }, { error("Removed resource") }))
    }
    @Test fun allAffectedConsumersUseNamesInsteadOfBuildTimeIds() {
        val root = listOf(File("src/main"), File("app/src/main")).first { it.isDirectory }
        for (name in listOf("SettingsAppInfoCardHooker", "BatteryCycleCountHooker", "GameAssistantDeveloperController",
            "SoftwareUpdateMenuController", "LauncherGridApplySession", "FaceTapUnlockHooker")) {
            val source = File(root, "java/com/mi/onextbox/lsp/$name.kt").readText()
            assertFalse(name, source.contains("import com.mi.onextbox.R"))
            assertTrue(name, source.contains("ModuleStrings.get("))
        }
        val helper = File(root, "java/com/mi/onextbox/lsp/ModuleStrings.kt").readText()
        assertTrue(helper.contains("createConfigurationContext(context.resources.configuration)"))
        assertTrue(helper.contains("resources.getIdentifier(it, \"string\", PACKAGE)"))
        assertTrue(helper.contains("resources.getString(it, *args)"))
        val keep = File(root, "res/raw/module_strings_keep.xml").readText()
        assertTrue(keep.contains("@string/settings_app_info_*"))
        assertTrue(keep.contains("@string/ota_details_*"))
    }
}
