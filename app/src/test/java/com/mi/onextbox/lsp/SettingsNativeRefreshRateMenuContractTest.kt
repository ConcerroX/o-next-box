package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** OEM classes cannot run on the JVM; keep the writer, native UI and rollback boundaries explicit. */
class SettingsNativeRefreshRateMenuContractTest {
    @Test fun usesExistingToggleAndNativeWriterRatherThanInjectingModesOrPreferences() {
        val code = source()
        assertTrue(code.contains("LspConfig.isSettingsRefreshRateUnlockedXposed()"))
        assertTrue(code.contains("apply.invoke(owner, SettingsRefreshRateMenuRules.RATE_90)"))
        assertFalse(code.contains("Settings.Secure.put"))
        assertFalse(code.contains("setOnPreferenceChangeListener"))
        assertFalse(code.contains("newInstance("))
        assertFalse(code.contains("choiceModeRateToWmsMode"))
    }

    @Test fun resolvesFullSignatureContractBeforeInstallingAnyHookAndCleansUpOnFailure() {
        val code = source()
        assertTrue(code.indexOf("val apply = method(") < code.indexOf("ModernHookRegistry.installCompat("))
        assertTrue(code.indexOf("val normalize = method(") < code.indexOf("ModernHookRegistry.installCompat("))
        assertTrue(code.contains("check(it.returnType == result)"))
        assertTrue(code.contains("ModernHookRegistry.unhookPrefix(prefix)"))
    }

    @Test fun stale90ChoiceNeverFallsThroughToNative120Fallback() {
        val selection = source().substringAfter("prefix + \"selection\"")
            .substringBefore("prefix + \"classification\"")
        assertTrue(selection.contains("if (param.args.getOrNull(2) != SettingsRefreshRateMenuRules.VALUE_90) return"))
        assertTrue(selection.indexOf("param.result = false") < selection.indexOf("if (!enabled(context))"))
        assertTrue(selection.indexOf("apply.invoke(") < selection.indexOf("param.result = true"))
    }

    @Test fun nativeSelectionSummaryAndReturningToHighRemainConsistent() {
        val code = source()
        assertTrue(code.contains("prefix + \"classification\""))
        assertTrue(code.contains("currentRates.get(null)"))
        assertTrue(code.contains("setValue.invoke(menu, SettingsRefreshRateMenuRules.VALUE_90)"))
        assertTrue(code.contains("choice.invoke(null, context) == SettingsRefreshRateMenuRules.RATE_90"))
        assertTrue(code.contains("display.mode.spec(), display.supportedModes.map"))
    }

    private fun source(): String = listOf(
        File("src/main/java/com/mi/onextbox/lsp/SettingsNativeRefreshRateMenu.kt"),
        File("app/src/main/java/com/mi/onextbox/lsp/SettingsNativeRefreshRateMenu.kt"),
    ).first { it.isFile }.readText().replace("\r\n", "\n")
}
