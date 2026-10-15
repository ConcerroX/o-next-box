package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class QsControlCenterCornerContractTest {
    private fun source(name: String) = listOf(File("src/main/java/com/mi/onextbox/lsp/$name.kt"),
        File("app/src/main/java/com/mi/onextbox/lsp/$name.kt")).first { it.isFile }.readText()

    @Test fun mediaBackgroundAndItsIndependentLightBorderCacheFollowTheSamePath() {
        val hook = source("QsTileCornerHooker")
        assertTrue(hook.contains("com.oplus.systemui.qs.media.OplusQsBaseMediaPanelView"))
        assertTrue(hook.contains("com.oplus.systemui.qs.media.multilight.OplusQsMediaBackgroundDrawable"))
        assertTrue(hook.contains("\"getViewRadius\", \"getContainerCornerRadius\""))
        assertTrue(hook.contains("\"\$TAG:media-draw\""))
        assertTrue(hook.contains("syncMediaLight(drawable, accepted)"))
        assertTrue(hook.contains("syncMediaLight(drawable, state.original)"))
        assertTrue(hook.contains("val effective = Reflect.callMethod(drawable, \"getPathProvider\") ?: provider"))
        assertTrue(hook.contains("\"setCornerParams\", radius, weight"))
        assertFalse(hook.contains("setBackgroundColor"))
    }
    @Test fun sliderUpdatesUseNativeSetterAndNeverChangeSystemValuesOrGlobalResources() {
        val slider = source("QsControlCenterSliderCorners")
        assertTrue(source("QsTileCornerHooker").contains("QsControlCenterSliderCorners.hook(loader)"))
        assertTrue(slider.contains("QsControlCenterCornerTargets.isMainSlider(view.javaClass.name)"))
        assertTrue(slider.contains("\"setCornerRadius\", radius"))
        assertTrue(slider.contains("scope.paint(paintedProgress, paintedBackground)"))
        assertTrue(slider.contains("QsControlCenterCornerTargets.SEEK_BAR"))
        assertTrue(slider.contains("ModernHookRegistry.installFast(\"\$TAG:seek-draw\""))
        assertTrue(slider.contains("radius == previous"))
        for (unsafe in listOf("setProgress", "setVolume", "AudioManager", "Settings.System.put", "Resources::class", "View::class.java", "setStatic", "getStatic")) {
            assertFalse(unsafe, slider.contains(unsafe))
        }
    }
    @Test fun sharedSmoothRoundParametersAreCopiedNotModified() {
        val slider = source("QsControlCenterSliderCorners")
        val round = slider.substringAfter("private fun applyVisualRadius").substringBefore("private fun restore")
        assertTrue(round.contains("Reflect.callMethod(original, \"copy\", radiusDp, radius, weight, false)"))
        assertFalse(round.contains("\"setRadius\""))
        assertFalse(round.contains("\"setWeight\""))
        assertFalse(round.contains("setObjectField"))
        assertTrue(round.contains("finally"))
    }
    @Test fun constructionKeepsNativeCachedValuesAndRetirementRestoresCurrentTheme() {
        val slider = source("QsControlCenterSliderCorners")
        assertTrue(slider.contains("if (view.width <= 0 || view.height <= 0) return null"))
        assertTrue(slider.contains("WeakHashMap<View, Float>()"))
        assertTrue(slider.contains("HotReloadLifecycle.register(\"qs:slider-corners\", detach ="))
        assertTrue(slider.contains("nativeRadius.set(true)"))
        assertTrue(slider.contains("Reflect.callMethod(view, \"getRadius\")"))
        assertTrue(slider.contains("if (previous == null) nativeRadius.remove() else nativeRadius.set(previous)"))
        assertTrue(slider.contains("applied.remove(view)"))
        assertTrue(slider.contains("Reflect.setObjectField(seek, name, value)"))
    }
    @Test fun defaultRadiusAndPreferenceKeysRemainCompatibleWithTheAlreadyInstalledVersion() {
        val settings = source("QsTileCornerSettings")
        assertTrue(settings.contains("const val ENABLED = \"systemui_qs_tile_corner_enabled\""))
        assertTrue(settings.contains("const val RADIUS = \"systemui_qs_tile_corner_radius\""))
        assertTrue(settings.contains("const val DEFAULT_DP = 24"))
        assertTrue(settings.contains("const val MAX_DP = 64"))
    }
    @Test fun sliderVisualChangesCannotMoveTheNativeTrackOrChangeTouchPadding() {
        val slider = source("QsControlCenterSliderCorners")
        val visual = slider.substringAfter("private fun applyVisualRadius").substringBefore("private fun restore")
        assertFalse(visual.contains("\"setCornerRadius\""))
        assertFalse(slider.contains("\"setPaddingVertical\""))
        assertFalse(slider.contains("\"setTransitionRadius\""))
        assertFalse(visual.contains("setObjectField"))
        assertFalse(visual.contains("\"setBackgroundRadius\""))
        assertFalse(slider.contains("it.name in setOf(\"getRadius\""))
        assertTrue(slider.contains("it.name == \"getViewRadius\""))
        assertTrue(visual.contains("\"getOnRadiusUpdateListener\""))
        assertTrue(visual.contains("\"updateBaseMixColorDrawableRadius\", radius"))
    }
    @Test fun scenarioTilesAreAdaptedWithoutChangingDetailPopupTilesOrTheirInnerIcons() {
        val hook = source("QsTileCornerHooker")
        assertTrue(hook.contains("com.oplus.systemui.qs.scenario.ui.template.ScenarioTileViewBase"))
        assertTrue(hook.contains("ScenarioTileViewOneXOne"))
        assertTrue(hook.contains("ScenarioTileViewTwoXOne"))
        assertTrue(hook.contains("ScenarioTileViewTwoXTwo"))
        assertTrue(hook.contains("ScenarioTileViewFourXOne"))
        assertTrue(hook.contains("scenarioWide -> Reflect.callMethod(view, \"getHighLightDrawableBg\")"))
        assertTrue(hook.contains("scenarioWide -> Reflect.callMethod(view, \"getTileBg\")"))
        assertFalse(hook.contains("findClassIfExists(\"com.oplus.systemui.qs.slider.detail.widget.QsDetailTileBaseView\""))
    }
    @Test fun mediaContentIsClippedToTheOwnedOutlineAndRestoredWithTheOriginalClipFlag() {
        val hook = source("QsTileCornerHooker")
        assertTrue(hook.contains("view.outlineProvider = custom"))
        assertTrue(hook.contains("view.clipToOutline = true"))
        assertTrue(hook.contains("if (view.outlineProvider === previous.custom)"))
        assertTrue(hook.contains("view.outlineProvider = previous.original"))
        assertTrue(hook.contains("view.clipToOutline = previous.clipped"))
        assertTrue(hook.contains("updateMediaClip(view, drawable)"))
        assertTrue(hook.contains("QsCornerClipOutline(state.radius)"))
        assertTrue(source("QsCornerClipOutline").contains("outline.setRoundRect(0, 0, view.width, view.height, radius)"))
    }
    @Test fun liveViewCallbacksAreWeakOwnedAndDetachedBeforeModuleRetirement() {
        val observers = source("QsCornerViewObservers")
        assertTrue(observers.contains("WeakHashMap<View, Guard>()"))
        assertTrue(observers.contains("private val target = WeakReference(view)"))
        assertTrue(observers.contains("WeakReference<ViewTreeObserver>"))
        assertTrue(observers.contains("removeOnPreDrawListener(this)"))
        assertTrue(observers.contains("removeOnAttachStateChangeListener(this)"))
        assertTrue(observers.contains("HotReloadLifecycle.withHook(fallback = { true })"))
        assertTrue(observers.contains("HotReloadLifecycle.registerReplay"))
        assertTrue(observers.contains("guards.keys.toTypedArray()"))
        assertTrue(source("QsTileCornerHooker").contains("observers::watch"))
        assertTrue(source("QsControlCenterSliderCorners").contains("observers::watch"))
    }
    @Test fun initialAndRebuiltLayoutsRepropagateCornersWithoutAFrameByFramePathReset() {
        val observers = source("QsCornerViewObservers")
        assertTrue(observers.contains("ViewTreeObserver.OnGlobalLayoutListener"))
        assertTrue(observers.contains("removeOnGlobalLayoutListener(this)"))
        assertTrue(source("QsTileCornerHooker").contains("onLayout = { updateSafely(it, provider, refreshChildren = true) }"))
    }
    @Test fun actualMediaSubclassesAreObservedEvenWhenNativeFactoriesInlineConstructors() {
        val hook = source("QsTileCornerHooker")
        assertTrue(hook.contains("QsCornerHierarchyObservers(\"qs:tile-discovery\")"))
        assertTrue(hook.contains(".any { it.name in types }"))
        assertTrue(hook.contains("observers.watch(view)"))
        assertTrue(hook.contains("com.oplus.systemui.plugins.qs.OplusQSRootView"))
    }
}
