package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SettingsRefreshRateSelectionContractTest {
    @Test fun initializesOnlyOnEnableAndSynchronizesTheToggleBeforeChangingNativeMode() {
        val setter = source("LspConfig").substringAfter("fun setSettingsRefreshRateUnlocked(")
            .substringBefore("fun isSettingsForceGlobalExtremeRefreshRateEnabled(")
        assertTrue(setter.contains("if (enabled && !wasEnabled)"))
        assertTrue(setter.contains("initialization != null && isSettingsRefreshRateUnlocked(context)"))
        assertTrue(setter.indexOf("setSyncedBooleanPreference(") < setter.indexOf("executeSyncCommands("))
        assertTrue(setter.indexOf("CHOICE_KEY} \${initialization.choiceHertz}") < setter.indexOf("MODE_KEY} \$mode"))
        assertFalse(setter.contains("getGameExceedRefreshRate"))
    }

    @Test fun legacyReadingIsBlockedBeforeOemCanWrite144AndOtherModesUseOriginalLogic() {
        val hook = source("SettingsRefreshRateSelectionHooker")
            .substringAfter("prefix + \"default-high\"").substringBefore("prefix + \"persisted-choice\"")
        assertTrue(hook.contains("override fun beforeHookedMethod"))
        assertTrue(hook.contains("LspConfig.isSettingsRefreshRateUnlockedXposed(), requestedMode"))
        assertTrue(hook.contains("param.result = SettingsRefreshRateSelectionRules.DEFAULT_HIGH_MODE"))
    }

    @Test fun readsPersistedChoiceWithoutWritingOverManualSelections() {
        val hook = source("SettingsRefreshRateSelectionHooker")
        assertTrue(hook.contains("if (!LspConfig.isSettingsRefreshRateUnlockedXposed()) return"))
        assertTrue(hook.contains("Settings.Secure.getInt("))
        assertTrue(hook.contains("if (stored > 0) param.result = stored"))
        assertFalse(hook.contains("Settings.Secure.put"))
        assertFalse(hook.contains("setChoiceModeRate"))
        assertTrue(hook.contains("ModernHookRegistry.unhookPrefix(prefix)"))
    }

    private fun source(name: String): String = listOf(
        File("src/main/java/com/mi/onextbox/lsp/$name.kt"),
        File("app/src/main/java/com/mi/onextbox/lsp/$name.kt"),
    ).first { it.isFile }.readText().replace("\r\n", "\n")
}
