package com.mi.onextbox.ui.screens

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class ConsolidatedStringsTest {
    private val root = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
    private val locales = listOf("values", "values-zh-rCN", "values-zh-rTW", "values-b+yue+Hant",
        "values-ja", "values-ja-rCN", "values-ko", "values-vi", "values-ru")
    private val groups = mapOf(
        "feature_aosp_back_panel_" to 2,
        "fluid_browser_" to 4,
        "launcher_custom_grid_" to 7,
        "phone_manager_" to 4,
        "side_key_" to 36,
        "ota_" to 40,
    )

    @Test fun consolidatedFilesHaveNoDuplicateStringsOrSplitFiles() {
        val splitFiles = listOf("aosp_back_panel.xml", "fluid_browser.xml", "launcher_custom_grid.xml",
            "phone_manager.xml", "side_key.xml", "software_update.xml")
        locales.forEach { locale ->
            strings(locale)
            splitFiles.forEach { name -> assertFalse("$locale/$name", File(root, "$locale/$name").exists()) }
        }
    }

    @Test fun allMergedGroupsRetainTheirKeysAndFormatArgumentsInEveryLocale() {
        val baseline = strings("values")
        val formatArgument = Regex("%(?:[0-9]+\\$)?[sd]")
        groups.forEach { (prefix, count) ->
            val expected = baseline.filterKeys { it.startsWith(prefix) }
            assertEquals(prefix, count, expected.size)
            locales.forEach { locale ->
                val translated = strings(locale).filterKeys { it.startsWith(prefix) }
                assertEquals("$locale:$prefix", expected.keys, translated.keys)
                expected.forEach { (name, value) ->
                    assertTrue("$locale:$name", translated.getValue(name).isNotBlank())
                    assertEquals("$locale:$name",
                        formatArgument.findAll(value).map { it.value }.toList(),
                        formatArgument.findAll(translated.getValue(name)).map { it.value }.toList())
                }
            }
        }
    }

    private fun strings(locale: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(root, "$locale/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        val values = (0 until nodes.length).map { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
        assertEquals("$locale contains duplicate strings", values.size, values.map { it.first }.toSet().size)
        return values.toMap()
    }
}
