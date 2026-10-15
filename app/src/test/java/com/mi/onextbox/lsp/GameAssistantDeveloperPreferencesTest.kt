package com.mi.onextbox.lsp

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class GameAssistantDeveloperPreferencesTest {
    private val adapter = GameAssistantDeveloperPreferences
    private val integerKey = "feel_adjust_v2_idle_fade_delay_sec_key"

    @Test fun readsSwitchesWithoutCallingUnsupportedMmkvGetAll() {
        val store = TypedStore(mapOf("on" to true, "off" to false))
        assertTrue(adapter.boolean(store.prefs, "on", false))
        assertFalse(adapter.boolean(store.prefs, "off", true))
        assertEquals(listOf("getBoolean", "getBoolean"), store.reads)
        assertEquals(0, store.edits)
    }

    @Test fun absentSwitchesDisplayNativeDefaultsWithoutPersistingThem() {
        val store = TypedStore()
        assertTrue(adapter.boolean(store.prefs, "on", true))
        assertFalse(adapter.boolean(store.prefs, "off", false))
        assertTrue(store.values.isEmpty())
        assertEquals(0, store.edits)
    }

    @Test fun stringSelectionsUseNativeStoredValue() {
        val store = TypedStore(mapOf("theme_key" to "dark", "king_of_glory_fourd_vibration" to ""))
        assertEquals("dark", adapter.selection(store.prefs, "theme_key", "light"))
        assertEquals("", adapter.selection(store.prefs, "king_of_glory_fourd_vibration", "1"))
        assertEquals(listOf("getString", "getString"), store.reads)
    }

    @Test fun absentStringSelectionKeepsResolvedResourceDefaultWithoutWriting() {
        val store = TypedStore()
        assertEquals("light", adapter.selection(store.prefs, "theme_key", "light"))
        assertEquals(0, store.edits)
        assertTrue(store.values.isEmpty())
    }

    @Test fun nativeIntegerSelectionIsNotReadAsAString() {
        val store = TypedStore(mapOf(integerKey to 10))
        assertEquals("10", adapter.selection(store.prefs, integerKey, "5"))
        assertEquals(listOf("getInt"), store.reads)
        assertEquals(0, store.edits)
    }

    @Test fun absentIntegerSelectionKeepsNativeDefaultWithoutWriting() {
        val store = TypedStore()
        assertEquals("5", adapter.selection(store.prefs, integerKey, "5"))
        assertEquals(0, store.edits)
        assertTrue(store.values.isEmpty())
    }

    @Test fun malformedIntegerDefaultsCannotTriggerAWrite() {
        val store = TypedStore()
        val failure = runCatching { adapter.selection(store.prefs, integerKey, "@resource") }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(store.reads.isEmpty())
        assertEquals(0, store.edits)
    }

    @Test fun switchWritesOnlyItsBooleanKey() {
        val store = TypedStore(mapOf("unrelated" to 42))
        assertTrue(adapter.saveBoolean(store.prefs, "debug_info_toast_key", true))
        assertEquals(true, store.values["debug_info_toast_key"])
        assertEquals(42, store.values["unrelated"])
        assertEquals(listOf("putBoolean"), store.writes)
    }

    @Test fun integerChoiceRetainsNativeType() {
        val store = TypedStore()
        assertTrue(adapter.saveSelection(store.prefs, integerKey, "10"))
        assertEquals(10, store.values[integerKey])
        assertEquals(listOf("putInt"), store.writes)
        assertEquals("10", adapter.selection(store.prefs, integerKey, "5"))
    }

    @Test fun numericStringChoicesAreNotMistakenForIntegerOptions() {
        val store = TypedStore()
        assertTrue(adapter.saveSelection(store.prefs, "assistant_tool_style_mock_key", "0"))
        assertEquals("0", store.values["assistant_tool_style_mock_key"])
        assertEquals(listOf("putString"), store.writes)
    }

    @Test fun malformedIntegerChoicesAreRejectedBeforeEditing() {
        val store = TypedStore(mapOf(integerKey to 5))
        val failure = runCatching { adapter.saveSelection(store.prefs, integerKey, "invalid") }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertEquals(5, store.values[integerKey])
        assertEquals(0, store.edits)
    }

    @Test fun failedCommitIsReportedAndDoesNotFabricateSuccess() {
        val store = TypedStore(mapOf("switch" to false), commitSucceeds = false)
        assertFalse(adapter.saveBoolean(store.prefs, "switch", true))
        assertFalse(adapter.saveSelection(store.prefs, integerKey, "10"))
        assertFalse(adapter.saveSelection(store.prefs, "theme_key", "dark"))
        assertEquals(mapOf("switch" to false), store.values)
    }

    @Test fun legacyTypeMismatchIsNotSilentlyOverwrittenOnOpen() {
        val store = TypedStore(mapOf(integerKey to "10"))
        val failure = runCatching { adapter.selection(store.prefs, integerKey, "5") }.exceptionOrNull()
        assertTrue(failure is ClassCastException)
        assertEquals("10", store.values[integerKey])
        assertEquals(0, store.edits)
    }

    /** Mirrors MMKV's supported typed contract and explicitly rejects getAll(). */
    private class TypedStore(initial: Map<String, Any> = emptyMap(), private val commitSucceeds: Boolean = true) {
        val values = initial.toMutableMap()
        val reads = mutableListOf<String>()
        val writes = mutableListOf<String>()
        var edits = 0
        val prefs = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getAll" -> throw UnsupportedOperationException("MMKV getAll is not supported")
                "getBoolean", "getInt", "getString" -> {
                    reads += method.name
                    val value = values[args[0] as String] ?: args[1]
                    val expected = when (method.name) {
                        "getBoolean" -> Boolean::class.javaObjectType
                        "getInt" -> Int::class.javaObjectType
                        else -> String::class.java
                    }
                    if (value != null && !expected.isInstance(value)) throw ClassCastException("Wrong native type")
                    value
                }
                "edit" -> { edits++; editor() }
                else -> error("Unexpected preferences operation: ${method.name}")
            }
        } as SharedPreferences

        private fun editor(): SharedPreferences.Editor {
            val pending = mutableMapOf<String, Any>()
            return Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
                when (method.name) {
                    "putBoolean", "putInt", "putString" -> {
                        writes += method.name
                        pending[args[0] as String] = args[1]
                        proxy
                    }
                    "commit" -> {
                        if (commitSucceeds) values.putAll(pending)
                        commitSucceeds
                    }
                    else -> error("Unexpected editor operation: ${method.name}")
                }
            } as SharedPreferences.Editor
        }
    }
}
