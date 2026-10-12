package com.mi.onextbox.ui.screens

import java.text.Collator
import java.util.Locale

internal data class MediaBlacklistApp(val packageName: String, val label: String)

internal fun mediaBlacklistApps(
    apps: List<MediaBlacklistApp>,
    selected: Set<String>,
    query: String,
    locale: Locale,
): List<MediaBlacklistApp> {
    val known = apps.map { it.packageName }.toSet()
    val search = query.trim()
    val collator = Collator.getInstance(locale)
    return (apps + selected.filterNot { it in known }.map { MediaBlacklistApp(it, it) })
        .filter { search.isEmpty() || it.label.contains(search, true) || it.packageName.contains(search, true) }
        .sortedWith { a, b ->
            val priority = (a.packageName !in selected).compareTo(b.packageName !in selected)
            if (priority != 0) priority
            else collator.compare(a.label, b.label).takeIf { it != 0 }
                ?: a.packageName.compareTo(b.packageName)
        }
}
