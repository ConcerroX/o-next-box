package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class KeyboardFlagEmojiRulesTest {
    @Test fun onlyTheEnabledFlagCategoryIsChanged() {
        assertTrue(KeyboardFlagEmojiRules.shouldAugment(true, "flags"))
        assertFalse(KeyboardFlagEmojiRules.shouldAugment(false, "flags"))
        listOf(null, "recent_used", "symbols", "faces_emotion", "FLAGS", 12).forEach {
            assertFalse(KeyboardFlagEmojiRules.shouldAugment(true, it))
        }
    }

    @Test fun sequencesUseTheCompleteUnicodeEmoji() {
        assertArrayEquals(intArrayOf(0x1F1F9, 0x1F1FC), points(0))
        assertArrayEquals(intArrayOf(0x1F3F3, 0xFE0F, 0x200D, 0x1F308), points(1))
        assertArrayEquals(intArrayOf(0x1F3F3, 0xFE0F, 0x200D, 0x26A7, 0xFE0F), points(2))
    }

    @Test fun insertsAllThreeAfterMacauInTheRequestedOrderAndRetainsStockOrder() {
        val source = listOf("🇨🇳", "🇭🇰", "🇲🇴", "🏁", "🇺🇸")
        val result = supplement(source)
        assertEquals(source.take(3) + KeyboardFlagEmojiRules.flags.map { it.value } + source.drop(3), result)
        assertEquals(listOf("🇨🇳", "🇭🇰", "🇲🇴", "🏁", "🇺🇸"), source)
    }

    @Test fun existingFlagsAreReusedWithoutCreatingDuplicates() {
        val stockRainbow = Item(KeyboardFlagEmojiRules.flags[1].value, "stock")
        val stockTransgender = Item(KeyboardFlagEmojiRules.flags[2].value, "stock")
        val other = Item("🇨🇳", "stock")
        val macau = Item("🇲🇴", "stock")
        val source = listOf(stockRainbow, other, macau, stockTransgender, stockRainbow)
        val result = KeyboardFlagEmojiRules.supplement(source, { it.value }, { Item(it.value, "new") })
        assertSame(other, result[0])
        assertSame(macau, result[1])
        assertSame(stockRainbow, result[3])
        assertSame(stockTransgender, result[4])
        assertEquals(5, result.size)
        assertEquals(5, source.size)
    }

    @Test fun variationSelectorsDoNotDuplicateExistingFlags() {
        val rainbow = "\uD83C\uDFF3\u200D\uD83C\uDF08"
        val transgender = "\uD83C\uDFF3\u200D\u26A7"
        val result = supplement(listOf(rainbow, "🇲🇴\uFE0F", transgender))
        assertEquals(listOf("🇲🇴\uFE0F", KeyboardFlagEmojiRules.flags[0].value, rainbow, transgender), result)
    }

    @Test fun repeatedSupplementationIsIdempotent() {
        val result = supplement(listOf("🇨🇳", "🇲🇴", "🏴", "🏳️"))
        assertEquals(result, supplement(result))
    }

    @Test fun missingMacauFallsBackToTheEndWithoutReorderingStockFlags() {
        val source = listOf("🇨🇳", "🏁", "🇺🇸")
        assertEquals(source + KeyboardFlagEmojiRules.flags.map { it.value }, supplement(source))
    }

    @Test fun filteredEmptyListsCanBeSupplemented() {
        assertEquals(KeyboardFlagEmojiRules.flags.map { it.value }, supplement(emptyList()))
    }

    private data class Item(val value: String, val origin: String)
    private fun points(index: Int) = KeyboardFlagEmojiRules.flags[index].value.codePoints().toArray()
    private fun supplement(source: List<String>) = KeyboardFlagEmojiRules.supplement(source, { it }, { it.value })
}
