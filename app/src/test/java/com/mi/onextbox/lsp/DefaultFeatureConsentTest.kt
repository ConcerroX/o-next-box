package com.mi.onextbox.lsp

import android.content.SharedPreferences
import com.mi.onextbox.lsp.compat.HookConfigSnapshot
import com.mi.onextbox.lsp.compat.MODERN_PREFERENCES_READY_KEY
import java.io.File
import java.lang.reflect.Proxy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DefaultFeatureConsentTest {
    private val affectedKeys = listOf("native_notify_icon_enabled", "aod_panoramic_support", "aod_settings_switch",
        "aod_single_click_block", "assistant_international_power_chord_enabled", "installer_intercept_system",
        "installer_remove_default_app_policy")

    @Before fun before() = clearSnapshot()
    @After fun after() = clearSnapshot()

    @Test fun emptyConfigurationHasNoFormerlyEnabledUiOrHookFeatures() {
        val empty = preferences(emptyMap())
        val ui = LspConfig.readCachedUiSnapshot(empty)
        assertFalse(ui.nativeNotifyIconEnabled)
        assertFalse(ui.aodAllDayUnlockEnabled)
        assertFalse(ui.aodSingleClickBlockEnabled)
        attach(emptyMap())
        assertEquals(List(6) { false }, hookValues())
        assertTrue(LspConfig.InstallerFeature.entries.none { it.defaultValue })
    }

    @Test fun explicitlySavedEnabledChoicesArePreservedWithoutMigrationWrites() {
        val values = affectedKeys.associateWith { true }
        val store = preferences(values)
        val ui = LspConfig.readCachedUiSnapshot(store)
        assertTrue(ui.nativeNotifyIconEnabled)
        assertTrue(ui.aodAllDayUnlockEnabled)
        assertTrue(ui.aodSingleClickBlockEnabled)
        attach(values)
        assertEquals(List(6) { true }, hookValues())
        assertEquals(values, store.all)
    }

    @Test fun explicitlySavedDisabledChoicesStayDisabledAndUnrelatedChoicesSurvive() {
        val values = affectedKeys.associateWith { false } + ("game_assistant_all_skins" to true)
        val store = preferences(values)
        attach(values)
        assertEquals(List(6) { false }, hookValues())
        assertEquals(true, HookConfigSnapshot.boolean("game_assistant_all_skins", false))
        assertEquals(values, store.all)
    }

    @Test fun legacyPartialAodKeysCannotImplicitlyOptInAMissingChoice() {
        for (key in listOf("aod_panoramic_support", "aod_settings_switch")) {
            val values = mapOf(key to true)
            assertFalse(LspConfig.readCachedUiSnapshot(preferences(values)).aodAllDayUnlockEnabled)
            attach(values)
            assertFalse(LspConfig.isAodAllDayUnlockEnabledXposed())
        }
    }

    @Test fun snapshotUiLiveReadersHookFallbacksAndResetShareTheOffDefaults() {
        val config = source("LspConfig.kt")
        for (constant in listOf("DEFAULT_NATIVE_NOTIFY_ICON", "DEFAULT_ASSISTANT_INTERNATIONAL_POWER_CHORD",
            "DEFAULT_AOD_PANORAMIC_SUPPORT", "DEFAULT_AOD_SETTINGS_SWITCH", "DEFAULT_AOD_SINGLE_CLICK_BLOCK")) {
            assertTrue(constant, config.contains("private const val $constant = false"))
        }
        assertTrue(config.contains("LspPreferenceStore.readBoolean(context, KEY_NATIVE_NOTIFY_ICON, DEFAULT_NATIVE_NOTIFY_ICON)"))
        assertTrue(config.contains("LspPreferenceStore.readBoolean(context, KEY_ASSISTANT_INTERNATIONAL_POWER_CHORD, DEFAULT_ASSISTANT_INTERNATIONAL_POWER_CHORD)"))
        assertTrue(config.contains("setNativeNotifyIconEnabled(context, DEFAULT_NATIVE_NOTIFY_ICON)"))
        assertTrue(config.contains("setAssistantInternationalPowerChordEnabled(context, DEFAULT_ASSISTANT_INTERNATIONAL_POWER_CHORD)"))
        assertTrue(config.contains("setAodAllDayUnlockEnabled(context, DEFAULT_AOD_PANORAMIC_SUPPORT)"))
        assertTrue(config.contains("setAodSingleClickBlockEnabled(context, DEFAULT_AOD_SINGLE_CLICK_BLOCK)"))
        assertTrue(config.contains("InstallerFeature.entries.forEach { setInstallerFeatureEnabled(context, it, it.defaultValue) }"))
        assertTrue(config.contains("getOrDefault(DEFAULT_NATIVE_NOTIFY_ICON)"))
        assertTrue(config.contains("getOrDefault(DEFAULT_ASSISTANT_INTERNATIONAL_POWER_CHORD)"))
        assertFalse(config.contains("KEY_NATIVE_NOTIFY_ICON, true"))
        assertFalse(config.contains("KEY_ASSISTANT_INTERNATIONAL_POWER_CHORD, true"))
    }

    private fun hookValues() = listOf(LspConfig.isNativeNotifyIconEnabledXposed(), LspConfig.isAodAllDayUnlockEnabledXposed(),
        LspConfig.isAodSingleClickBlockEnabledXposed(), LspConfig.isAssistantInternationalPowerChordEnabledXposed(),
        LspConfig.isInstallerFeatureEnabledXposed(LspConfig.InstallerFeature.InterceptSystem),
        LspConfig.isInstallerFeatureEnabledXposed(LspConfig.InstallerFeature.RemoveDefaultAppPolicy))

    private fun attach(values: Map<String, Boolean>) {
        check(HookConfigSnapshot.attach(preferences(values + (MODERN_PREFERENCES_READY_KEY to true))))
    }

    private fun clearSnapshot() {
        check(HookConfigSnapshot.detach())
        check(HookConfigSnapshot.attach(preferences(emptyMap())))
        check(HookConfigSnapshot.detach())
    }

    /** Read-only JVM store; any accidental preference write fails the regression. */
    private fun preferences(values: Map<String, Any>): SharedPreferences = Proxy.newProxyInstance(
        SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java),
    ) { proxy, method, args ->
        when (method.name) {
            "getAll" -> HashMap(values)
            "getBoolean", "getInt", "getLong", "getFloat", "getString", "getStringSet" -> values[args!![0]] ?: args[1]
            "contains" -> values.containsKey(args!![0])
            "registerOnSharedPreferenceChangeListener", "unregisterOnSharedPreferenceChangeListener" -> null
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args!![0]
            "toString" -> "ReadOnlyFeaturePreferences"
            else -> error("Unexpected preference operation: ${method.name}")
        }
    } as SharedPreferences

    private fun source(name: String) = listOf(File("src/main/java/com/mi/onextbox/lsp/$name"),
        File("app/src/main/java/com/mi/onextbox/lsp/$name")).first { it.isFile }.readText()
}
