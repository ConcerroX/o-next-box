package com.mi.onextbox.lsp

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class QsTileCornerContractTest {
    private fun file(path: String) = listOf(File("src/$path"), File("app/src/$path")).first { it.isFile }
    private fun source(name: String) = file("main/java/com/mi/onextbox/$name.kt").readText()

    @Test fun tileDrawDiscoveryAvoidsCompatibilityAllocationAndAlwaysCallsNativeDraw() {
        val draw = source("lsp/QsTileCornerHooker")
            .substringAfter("type.declaredMethods.filter { it.name == \"dispatchDraw\" }")
            .substringBefore("type.declaredMethods.filter { it.name in setOf")
        assertTrue(draw.contains("ModernHookRegistry.installFast"))
        assertFalse(draw.contains("installCompat"))
        assertTrue(draw.contains("observers::watch"))
        assertTrue(draw.contains("chain.proceed()"))
    }

    @Test fun hookInstallationIsSystemUiOnlyAndDoesNotInitializeStaticConfiguration() {
        val entry = source("lsp/OosLspModuleEntry")
        assertEquals(1, Regex("QsTileCornerHooker.hook").findAll(entry).count())
        assertTrue(entry.substringAfter("PACKAGE_SYSTEM_UI -> {").substringBefore("PACKAGE_SETTINGS").contains("QsTileCornerHooker.hook(classLoader)"))
        val hook = source("lsp/QsTileCornerHooker")
        assertFalse(hook.contains("getStaticIntField"))
        assertFalse(hook.contains("getStaticObjectField"))
        assertFalse(hook.contains("R\\\$dimen"))
        assertTrue(hook.contains("findClassIfExists"))
        assertTrue(hook.contains("deoptimize(method)"))
    }
    @Test fun onlyNativeTileBackgroundProvidersAreChangedNotSharedPoolsOrFrameworkViews() {
        val hook = source("lsp/QsTileCornerHooker")
        for (name in listOf("OplusQSResizeableTileView", "OneXOne", "TwoXOne", "TwoXTwo", "OplusQSResizeableThreeStageView", "OplusQSTileBaseView", "OplusQSHighlightTileView")) {
            assertTrue(name, hook.contains(name))
        }
        assertTrue(hook.contains("\"getTransitionDrawable\""))
        assertTrue(hook.contains("\"mBgDrawable\""))
        assertTrue(hook.contains("Reflect.newInstance(provider, radius, weight)"))
        assertTrue(hook.contains("Reflect.callMethod(original, \"getCornerWeight\", drawable)"))
        for (unsafe in listOf("StdQSResPool", "SepQSResPool", "View::class.java", "GradientDrawable::class.java", "setBackground(", "setBackgroundColor(", "getStatic")) {
            assertFalse(unsafe, hook.contains(unsafe))
        }
    }
    @Test fun originalProviderIsOwnedByIdentityAndRestoredOnDisableAndReload() {
        val hook = source("lsp/QsTileCornerHooker")
        assertTrue(hook.contains("WeakHashMap<Drawable, QsTileCornerOwnership<Any>>()"))
        assertTrue(hook.contains("state.shouldRestore(current)"))
        assertTrue(hook.contains("if (dp == null)"))
        assertTrue(hook.contains("restore(drawable, state)"))
        assertTrue(hook.contains("owned.remove(drawable)"))
        assertTrue(hook.contains("HotReloadLifecycle.register(\"qs:tile-corners\", detach ="))
        assertTrue(hook.contains("\"setPathProvider\", state.original"))
        assertTrue(hook.contains("HotReloadLifecycle.isRetiring"))
    }
    @Test fun temporaryPressProviderIsNotCapturedAndAnimatedRadiusIsNotFrozen() {
        val hook = source("lsp/QsTileCornerHooker")
        assertTrue(hook.contains("Reflect.getObjectField(delegate, \"pathProvider\")"))
        assertTrue(hook.contains("\"getDeformPathProvider\") != null"))
        assertFalse(hook.contains("\"setBlockPathProvider\""))
        assertFalse(hook.contains("\"setDeformFraction\""))
        assertFalse(hook.contains("\"setAnimationEnable\""))
        assertTrue(hook.contains("state?.matches(current, radius, weight) == true"))
        assertTrue(hook.contains("next.accepted(accepted)"))
        assertTrue(hook.contains("if (bg == null || bg.width <= 0 || bg.height <= 0) return@runCatching"))
        assertFalse(hook.contains("setObjectField(view, \"viewRadius\""))
        assertTrue(hook.contains("TileFixedRadiusShareTransitionProperty"))
    }
    @Test fun configurationDefaultsOffAndToggleDoesNotResetRadius() {
        val settings = source("lsp/QsTileCornerSettings")
        assertTrue(settings.contains("it.getBoolean(ENABLED, false)"))
        assertTrue(settings.contains("HookConfigSnapshot.boolean(ENABLED, false) == true"))
        val toggle = settings.substringAfter("fun setEnabled(").substringBefore("fun setRadius(")
        assertFalse(toggle.contains("putInt"))
        val slider = settings.substringAfter("fun setRadius(")
        assertFalse(slider.contains("putBoolean"))
        assertTrue(slider.contains("QsTileCornerRules.normalize(radius)"))
        assertTrue(slider.contains("LspConfig.syncReadableState(context)"))
    }
    @Test fun sharedDualUiUsesAnimatedJoinedCardsSliderAndExplicitReset() {
        val ui = source("ui/screens/UiCustomizationSettings").substringAfter("internal fun QsTileCornerSettingsGroup()").substringBefore("internal fun StatusBarLayoutSettingsGroup")
        assertTrue(ui.contains("FeatureAnimatedGroup(animateChanges = true)"))
        assertTrue(ui.contains("visible = value.enabled"))
        assertTrue(ui.contains("FeatureSliderRow("))
        assertTrue(ui.contains("hasDividerAbove = above, hasDividerBelow = below"))
        assertTrue(ui.contains("onReset ="))
        assertTrue(ui.contains("setRadius(context, QsTileCornerRules.DEFAULT_DP)"))
        assertTrue(source("ui/screens/SystemUiFeaturesPage").substringAfter("FeaturePageMode.SystemUiControlCenter ->").contains("QsTileCornerSettingsGroup()"))
        for (name in listOf("FeatureSearchIndex", "EnabledFeatureCatalog", "EnabledFeatureActions")) {
            assertTrue(name, source("ui/screens/$name").contains("SharedFeatureDefinitions.qsCorner"))
        }
    }
    @Test fun allNineLocalesContainCompleteTextWithoutTrailingPeriods() {
        for (locale in listOf("values", "values-zh-rCN", "values-zh-rTW", "values-b+yue+Hant", "values-ja", "values-ja-rCN", "values-ko", "values-ru", "values-vi")) {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file("main/res/$locale/strings.xml")).getElementsByTagName("string")
            val strings = (0 until nodes.length).associate { nodes.item(it).attributes.getNamedItem("name").nodeValue to nodes.item(it).textContent }
            for (key in listOf("group", "title", "summary", "radius").map { "qs_tile_corner_$it" }) {
                assertTrue("$locale/$key", strings[key]?.isNotBlank() == true)
                assertFalse("$locale/$key", strings.getValue(key).endsWith(".") || strings.getValue(key).endsWith("。"))
            }
        }
    }
}
