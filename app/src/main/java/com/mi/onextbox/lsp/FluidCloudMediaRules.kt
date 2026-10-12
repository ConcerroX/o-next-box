package com.mi.onextbox.lsp

internal object FluidCloudMediaRules {
    private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+")

    fun isPackageName(value: String): Boolean = packagePattern.matches(value)

    fun normalize(packages: Set<String>): Set<String> = packages.asSequence()
        .map(String::trim).filter(::isPackageName).toSortedSet()

    fun <T> filter(entries: List<T>, blocked: Set<String>, packageName: (T) -> String?): List<T> {
        if (entries.isEmpty() || blocked.isEmpty()) return entries
        val filtered = entries.filter { packageName(it) !in blocked }
        return if (filtered.size == entries.size) entries else filtered
    }
}
