package com.mi.onextbox.lsp

import android.content.SharedPreferences

/** MMKV implements typed accessors, but deliberately does not implement getAll(). */
internal object GameAssistantDeveloperPreferences {
    fun boolean(prefs: SharedPreferences, key: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(key, defaultValue)

    fun selection(prefs: SharedPreferences, key: String, defaultValue: String): String =
        if (GameAssistantRules.developerIntKey(key)) {
            prefs.getInt(key, requireNotNull(defaultValue.toIntOrNull()) { "Invalid native integer default: $key" }).toString()
        } else prefs.getString(key, defaultValue) ?: defaultValue

    fun saveBoolean(prefs: SharedPreferences, key: String, value: Boolean): Boolean =
        prefs.edit().putBoolean(key, value).commit()

    fun saveSelection(prefs: SharedPreferences, key: String, value: String): Boolean =
        if (GameAssistantRules.developerIntKey(key)) {
            val number = requireNotNull(value.toIntOrNull()) { "Invalid native integer choice: $key" }
            prefs.edit().putInt(key, number).commit()
        } else prefs.edit().putString(key, value).commit()
}
