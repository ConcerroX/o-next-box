package com.mi.onextbox.lsp

import android.content.Context

internal object NetworkIdentityCatalog {
    data class Entry(val region: String, val numeric: String, val title: String) {
        val name: String get() {
            val alias = aliases[numeric] ?: title
            val result = StringBuilder()
            alias.codePoints().toArray().forEach { code ->
                val next = String(Character.toChars(code))
                if ((result.toString() + next).toByteArray().size <= 40) result.append(next)
                else return result.toString().trimEnd()
            }
            return result.toString()
        }
        fun selection(enabled: Boolean) = NetworkIdentitySettings.Slot(enabled, region, numeric, name)
    }
    val popular = listOf("46692", "46697", "46601", "45400", "45403", "45406", "45412",
        "46000", "46001", "46011", "46015", "310260", "310410", "311480", "313340")
    private val aliases = mapOf("45400" to "csl", "45403" to "3 Hong Kong", "45406" to "SmarTone",
        "45412" to "China Mobile HK", "310260" to "T-Mobile", "310410" to "AT&T", "311480" to "Verizon",
        "313340" to "Boost Mobile")
    fun parse(lines: Sequence<String>): List<Entry> = lines.filter { it.isNotBlank() && !it.startsWith('#') }
        .mapNotNull {
            val parts = it.split('\t', limit = 3)
            if (parts.size != 3) null else Entry(parts[0], parts[1], parts[2]).takeIf { entry ->
                NetworkIdentityRules.valid(NetworkIdentityRules.Profile(entry.region, entry.numeric, entry.name))
            }
        }.distinctBy { it.region to it.numeric }.sortedWith(compareBy<Entry> { it.region }.thenBy { it.numeric }).toList()
    fun load(context: Context): List<Entry> = context.assets.open("network_identity_catalog.tsv").bufferedReader().use {
        parse(it.lineSequence())
    }
}
