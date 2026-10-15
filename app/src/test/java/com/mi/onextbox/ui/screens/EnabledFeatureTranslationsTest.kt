package com.mi.onextbox.ui.screens

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class EnabledFeatureTranslationsTest {
    @Test fun inventoryStringsHaveMatchingPlaceholdersInEverySupportedTranslation() {
        val root = File("src/main/res")
        fun strings(locale: String): Map<String, String> {
            val result = linkedMapOf<String, String>()
            File(root, locale).listFiles { f -> f.extension == "xml" }!!.forEach { file ->
                val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(file).getElementsByTagName("string")
                for (index in 0 until nodes.length) {
                    val node = nodes.item(index)
                    val key = node.attributes.getNamedItem("name").nodeValue
                    if (key.startsWith("enabled_features_")) {
                        assertNull("Duplicate $locale/$key", result.put(key, node.textContent))
                    }
                }
            }
            return result
        }
        val base = strings("values")
        val placeholders = Regex("%[0-9]+\\$[sd]")
        assertEquals(12, base.size)
        listOf("zh-rCN", "zh-rTW", "b+yue+Hant", "ja", "ja-rCN", "ko", "ru", "vi").forEach { locale ->
            val translated = strings("values-$locale")
            assertEquals(locale, base.keys, translated.keys)
            base.forEach { (key, value) ->
                assertTrue("$locale/$key", translated.getValue(key).isNotBlank())
                assertEquals("$locale/$key", placeholders.findAll(value).map { it.value }.toList(),
                    placeholders.findAll(translated.getValue(key)).map { it.value }.toList())
            }
        }
    }
}
