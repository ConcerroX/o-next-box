package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PhoneManagerRiskCloudContractTest {
    private val root = listOf(File("src/main"), File("app/src/main")).first { it.isDirectory }
    @Test fun onlyTheRiskAppVisualContextIsIntercepted() {
        val source = File(root, "java/com/mi/onextbox/lsp/PhoneManagerRiskCloud.kt").readText()
        assertTrue(source.contains("com.oplus.phonemanager.virusdetect.riskapptips.RiskAppTipsCardHelperKt"))
        assertTrue(source.contains("getDeclaredMethod(\"getVisualContext\", Context::class.java)"))
        assertTrue(source.contains("showRiskTips: no display context, fallback to notification"))
        assertTrue(source.contains("!PhoneManagerRiskCloudSettings.enabledXposed()) chain.proceed()"))
        assertTrue(source.contains(".deoptimize(it)"))
        assertFalse(source.contains("NotificationManager::class"))
        assertFalse(source.contains("getDeclaredMethod(\"sendNotification\""))
        assertFalse(source.contains("getDeclaredMethod(\"dismissRiskTips\""))
        assertTrue(source.contains("getBoolean(KEY, false)"))
        val entry = File(root, "java/com/mi/onextbox/lsp/OosLspModuleEntry.kt").readText()
        val scoped = entry.substringAfter("PhoneManagerSettings.PACKAGE_NAME -> {").substringBefore("}")
        assertTrue(scoped.contains("PhoneManagerRiskCloudHooker.hook(classLoader, target.sourceDir)"))
    }
    @Test fun translationsArePresentAndNonEmpty() {
        for (locale in listOf("values", "values-zh-rCN", "values-zh-rTW", "values-b+yue+Hant", "values-ja", "values-ja-rCN", "values-ko", "values-ru", "values-vi")) {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File(root, "res/$locale/phone_manager_risk_cloud_strings.xml"))
            val entries = document.getElementsByTagName("string")
            assertEquals(locale, 2, entries.length)
            for (index in 0 until entries.length) assertTrue(locale, entries.item(index).textContent.isNotBlank())
        }
    }
}
