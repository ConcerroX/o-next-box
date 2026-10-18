package com.mi.onextbox.lsp

import android.annotation.SuppressLint
import android.content.Context

/** Resource IDs belong to an APK version, not to the lifetime of a hooked host process. */
internal object ModuleStrings {
    private const val PACKAGE = "com.mi.onextbox"

    @SuppressLint("DiscouragedApi") // Resolve names against the same resource table used for reading.
    fun get(context: Context, name: String, vararg args: Any, fallback: String = "—"): String =
        runCatching {
            val localized = context.createPackageContext(PACKAGE, 0)
                .createConfigurationContext(context.resources.configuration)
            val resources = localized.resources
            ModuleStringLookup.resolve(name, fallback,
                identifier = { resources.getIdentifier(it, "string", PACKAGE) },
                read = { resources.getString(it, *args) })
        }.getOrDefault(fallback)
}

/** Never reuse an ID cached from a different Resources instance or APK generation. */
internal object ModuleStringLookup {
    fun resolve(name: String, fallback: String, identifier: (String) -> Int, read: (Int) -> String): String =
        runCatching {
            val id = identifier(name)
            if (id == 0) fallback else read(id)
        }.getOrDefault(fallback)
}
