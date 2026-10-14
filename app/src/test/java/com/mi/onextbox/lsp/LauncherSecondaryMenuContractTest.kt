package com.mi.onextbox.lsp

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class LauncherSecondaryMenuContractTest {
    private fun source(name: String) = listOf(
        File("src/main/java/com/mi/onextbox/lsp/$name"),
        File("app/src/main/java/com/mi/onextbox/lsp/$name"),
    ).first { it.isFile }.readText()

    @Test fun groupingIsClearedAtTheNotificationKeyQueryWithoutSuppressingContent() {
        val hook = source("LauncherGestureHooker.kt").substringAfter("private fun hookMenu(loader: ClassLoader)")
            .substringBefore("private fun hookTop(loader: ClassLoader)")
        assertTrue(hook.contains("popup.PopupDataProvider"))
        assertTrue(hook.contains("getDeclaredMethod(\"getNotificationKeysForItem\", item)"))
        assertTrue(hook.contains("getDeclaredField(\"mAddShortcutCount\")"))
        assertTrue(hook.contains("count.setInt(info, LauncherGestureRules.secondaryMenuGroupingCount(true, previous))"))
        val callback = hook.substringAfter("XposedInterface.Hooker { chain ->").substringBefore("if (LauncherGestureSettings.menuXposed()) {\n            // Keep compiled callers")
        assertTrue(callback.contains("chain.proceed()"))
        assertFalse(callback.contains("return@Hooker null"))
        assertFalse(callback.contains("emptyList"))
        assertFalse(callback.contains("isEnabled"))
        assertFalse(hook.contains("getDeclaredMethod(\"getShortcutCountForItem\""))
        assertFalse(hook.contains("GenericTouchController"))
        assertFalse(hook.contains("view.tag"))
    }

    @Test fun onlyPopupConstructionCallersAreDeoptimizedForTheEnabledSwitch() {
        val hook = source("LauncherGestureHooker.kt").substringAfter("private fun hookMenu(loader: ClassLoader)")
            .substringBefore("private fun hookTop(loader: ClassLoader)")
        assertTrue(hook.contains("if (LauncherGestureSettings.menuXposed())"))
        assertTrue(hook.contains("popup.model.ShortcutDataBuilder"))
        assertTrue(hook.contains("popup.presenter.Launcher3PopupPresenter"))
        assertTrue(hook.contains("popup.OplusPopupContainerWithArrow"))
        assertFalse(hook.contains("caller.declaredMethods.mapTo"))
    }

    @Test fun existingPreferenceIsPreservedAndStillDefaultsOff() {
        val settings = source("LauncherGestureSettings.kt")
        assertTrue(settings.contains("const val MENU = \"launcher_disable_icon_menu\""))
        assertTrue(settings.contains("getBoolean(MENU, false)"))
        assertTrue(settings.contains("HookConfigSnapshot.boolean(MENU, false)"))
    }

    @Test fun allTranslatedLabelsNameSecondaryMenusWithoutTheRemovedSuffix() {
        val resourceRoot = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        val locales = listOf("values", "values-zh-rCN", "values-zh-rTW", "values-b+yue+Hant", "values-ja-rCN",
            "values-ja", "values-ko", "values-vi", "values-ru")
        val expected = listOf(
            "Disable secondary menus" to "Hide secondary menus for app icons",
            "禁用二级菜单" to "隐藏应用图标的二级菜单",
            "停用次級選單" to "隱藏應用程式圖示的次級選單",
            "停用次級選單" to "隱藏應用程式圖示嘅次級選單",
            "停用次級選單" to "隱藏應用程式圖示的次級選單",
            "サブメニューを無効化" to "アプリアイコンのサブメニューを非表示",
            "보조 메뉴 비활성화" to "앱 아이콘의 보조 메뉴 숨기기",
            "Tắt menu phụ" to "Ẩn menu phụ của biểu tượng ứng dụng",
            "Отключить подменю" to "Скрыть подменю значков приложений",
        )
        locales.zip(expected).forEach { (locale, labels) ->
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File(resourceRoot, "$locale/strings.xml")).getElementsByTagName("string")
            val values = (0 until nodes.length).associate { index ->
                val node = nodes.item(index)
                node.attributes.getNamedItem("name").nodeValue to node.textContent
            }
            assertEquals(locale, labels.first, values["launcher_disable_icon_menu_title"])
            assertEquals(locale, labels.second, values["launcher_disable_icon_menu_summary"])
        }
    }
}
