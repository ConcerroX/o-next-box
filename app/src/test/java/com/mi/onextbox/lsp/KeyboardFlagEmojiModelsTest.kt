package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class KeyboardFlagEmojiModelsTest {
    class HostEmoji(val value: String, val name: String, var time: Long)
    class HostGroup(val emojis: List<HostEmoji>)
    class HostPair(val first: Any?, val second: Any?)

    private val models = KeyboardFlagEmojiModels(
        HostPair::class.java.getDeclaredConstructor(Any::class.java, Any::class.java),
        HostPair::class.java.getDeclaredField("first"), HostPair::class.java.getDeclaredField("second"),
        HostEmoji::class.java.getDeclaredConstructor(String::class.java, String::class.java, Long::class.javaPrimitiveType),
        HostEmoji::class.java.getDeclaredField("value"),
        HostGroup::class.java.getDeclaredConstructor(List::class.java), HostGroup::class.java.getDeclaredField("emojis"),
    )

    @Test fun originalAndDisplayedListsStayAlignedUsingHostModelTypes() {
        val stock = HostGroup(listOf(HostEmoji("🇨🇳", "中国", 12)))
        val macau = HostGroup(listOf(HostEmoji("🇲🇴", "澳门", 15)))
        val rainbow = HostGroup(listOf(HostEmoji(KeyboardFlagEmojiRules.flags[1].value, "彩虹旗", 18)))
        val source = HostPair(listOf(stock, macau, rainbow), listOf(stock, macau, rainbow))
        val result = models.supplement(source) as HostPair
        val first = result.first as List<*>
        val second = result.second as List<*>
        assertEquals(5, first.size)
        assertEquals(values(first), values(second))
        assertEquals(listOf("🇨🇳", "🇲🇴") + KeyboardFlagEmojiRules.flags.map { it.value }, values(first))
        assertSame(stock, first[0])
        assertSame(macau, first[1])
        assertSame(rainbow, first[3])
        assertEquals(3, (source.first as List<*>).size)
        assertEquals(12L, stock.emojis[0].time)
        assertEquals(15L, macau.emojis[0].time)
        assertEquals(18L, rainbow.emojis[0].time)
    }

    @Test fun unrelatedPayloadsAreNotChanged() {
        assertNull(models.supplement(null))
        assertNull(models.supplement("not an emoji panel"))
        assertNull(models.supplement(HostPair("not a list", emptyList<Any>())))
        assertNull(models.supplement(HostPair(listOf("wrong item"), emptyList<Any>())))
    }

    @Test fun supplementationRetainsAnEditableListForTheNativeCollector() {
        val result = models.supplement(HostPair(emptyList<Any>(), emptyList<Any>())) as HostPair
        assertTrue(result.first is ArrayList<*>)
        assertTrue(result.second is ArrayList<*>)
        val group = (result.first as List<*>)[0] as HostGroup
        assertEquals("台湾", group.emojis[0].name)
        assertEquals(0L, group.emojis[0].time)
    }

    private fun values(list: List<*>) = list.map { (it as HostGroup).emojis[0].value }
}
