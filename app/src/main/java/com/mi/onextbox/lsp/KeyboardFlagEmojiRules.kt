package com.mi.onextbox.lsp

internal object KeyboardFlagEmojiRules {
    private const val MACAU_FLAG = "\uD83C\uDDF2\uD83C\uDDF4"

    data class Flag(val value: String, val name: String)

    val flags = listOf(
        Flag("\uD83C\uDDF9\uD83C\uDDFC", "台湾"),
        Flag("\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08", "彩虹旗"),
        Flag("\uD83C\uDFF3\uFE0F\u200D\u26A7\uFE0F", "跨性别旗"),
    )

    fun shouldAugment(enabled: Boolean, category: Any?): Boolean = enabled && category == "flags"

    fun <T> supplement(
        original: List<T>,
        value: (T) -> String?,
        create: (Flag) -> T,
    ): ArrayList<T> {
        val wanted = flags.map { normalized(it.value) }.toSet()
        val existing = original.mapNotNull { item -> value(item)?.let { normalized(it) to item } }
            .filter { it.first in wanted }.groupBy({ it.first }, { it.second })
        return ArrayList<T>(original.size + flags.size).apply {
            original.forEach { item -> if (value(item)?.let(::normalized) !in wanted) add(item) }
            val anchor = indexOfFirst { value(it)?.let(::normalized) == MACAU_FLAG }
            addAll(if (anchor < 0) size else anchor + 1, flags.map { flag ->
                existing[normalized(flag.value)]?.firstOrNull() ?: create(flag)
            })
        }
    }

    private fun normalized(value: String): String = value.replace("\uFE0F", "").replace("\uFE0E", "")
}
