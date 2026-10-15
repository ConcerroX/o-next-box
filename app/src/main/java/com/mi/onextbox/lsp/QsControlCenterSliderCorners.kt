package com.mi.onextbox.lsp

import android.view.View
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect as Reflect
import io.github.libxposed.api.XposedInterface
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/** Customize visual corners without changing native vertical padding or seek geometry. */
internal object QsControlCenterSliderCorners {
    private const val TAG = "ONextBox-QsSliderCorner"
    private val applied = WeakHashMap<View, Float>()
    private val reported = ConcurrentHashMap.newKeySet<String>()
    private val nativeRadius = ThreadLocal<Boolean>()

    fun hook(loader: ClassLoader) {
        val observers = QsCornerViewObservers("qs:slider-views") { updateSafely(it) }
        QsControlCenterCornerTargets.mainSliders.forEach { name ->
            val type = Reflect.findClassIfExists(name, loader) ?: return@forEach
            type.declaredConstructors.forEach { constructor ->
                ModernHookRegistry.installCompat("$TAG:observe:${constructor.toGenericString()}", constructor, object : ModernMethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.throwable == null) (param.thisObject as? View)?.let(observers::watch)
                    }
                })
            }
            // getRadius drives native layout padding. Only the visual transition
            // getter may be overridden, after constructors have cached native values.
            type.declaredMethods.filter { it.name == "getViewRadius" && it.parameterCount == 0 }.forEach { method ->
                ModernHookRegistry.installCompat("$TAG:$name:${method.name}", method, object : ModernMethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.throwable != null || HotReloadLifecycle.isRetiring || nativeRadius.get() == true) return
                        val view = param.thisObject as? View ?: return
                        customRadius(view)?.let { param.result = it }
                    }
                })
                runCatching { ModernHookRuntime.requireModule().deoptimize(method) }
                    .onFailure { diagnose("Slider radius deoptimization", it) }
            }
            type.declaredMethods.filter { it.name in setOf("onAttachedToWindow", "onSizeChanged", "updateCornerRadius", "onDraw") }.forEach { method ->
                ModernHookRegistry.installCompat("$TAG:$name:${method.name}", method, object : ModernMethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.throwable == null) updateSafely(param.thisObject as? View)
                    }
                })
                runCatching { ModernHookRuntime.requireModule().deoptimize(method) }
                    .onFailure { diagnose("Slider refresh deoptimization", it) }
            }
        }
        val base = Reflect.findClassIfExists(QsControlCenterCornerTargets.BASE_SLIDER, loader)
        base?.declaredMethods?.filter { it.name == "setCornerRadius" && it.parameterCount == 1 }?.forEach { method ->
            ModernHookRegistry.installCompat("$TAG:after-native-radius", method, object : ModernMethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.throwable != null || HotReloadLifecycle.isRetiring || nativeRadius.get() == true) return
                    val view = param.thisObject as? View ?: return
                    updateSafely(view, force = true)
                }
            })
            runCatching { ModernHookRuntime.requireModule().deoptimize(method) }
                .onFailure { diagnose("Native radius setter deoptimization", it) }
        }
        // onDraw on the concrete seekbar is guaranteed to run even when its
        // FrameLayout parent skips its own onDraw. No framework-wide View hooks.
        Reflect.findClassIfExists(QsControlCenterCornerTargets.SEEK_BAR, loader)?.declaredMethods
            ?.filter { it.name == "onDraw" }?.forEach { method ->
                ModernHookRegistry.installFast("$TAG:seek-draw", method, XposedInterface.Hooker { chain ->
                    if (HotReloadLifecycle.isRetiring || nativeRadius.get() == true) return@Hooker chain.proceed()
                    if (QsTileCornerSettings.radiusXposed() == null && synchronized(applied) { applied.isEmpty() }) return@Hooker chain.proceed()
                    val seek = chain.thisObject as? View ?: return@Hooker chain.proceed()
                    val owner = generateSequence(seek.parent) { it.parent }.filterIsInstance<View>()
                        .firstOrNull { QsControlCenterCornerTargets.isMainSlider(it.javaClass.name) }
                    updateSafely(owner)
                    val radius = owner?.let(::customRadius) ?: return@Hooker chain.proceed()
                    var entered = false
                    try {
                        val progress = (Reflect.getObjectField(seek, QsSliderCornerPaintScope.PROGRESS) as Number).toFloat()
                        val background = (Reflect.getObjectField(seek, QsSliderCornerPaintScope.BACKGROUND) as Number).toFloat()
                        val progressBase = (Reflect.getObjectField(seek, "mProgressRadius") as Number).toFloat()
                        val backgroundBase = (Reflect.getObjectField(seek, "mBackgroundRadius") as Number).toFloat()
                        val paintedProgress = QsSliderCornerPaintScope.scaled(radius, progress, progressBase)
                        val paintedBackground = QsSliderCornerPaintScope.scaled(radius, background, backgroundBase)
                        val scope = QsSliderCornerPaintScope(
                            read = { (Reflect.getObjectField(seek, it) as Number).toFloat() },
                            write = { name, value -> Reflect.setObjectField(seek, name, value) })
                        scope.paint(paintedProgress, paintedBackground) {
                            entered = true
                            runCatching { Reflect.callMethod(seek, "updateBaseMixColorDrawableRadius", paintedProgress) }
                                .onFailure { diagnose("Slider paint material", it) }
                            chain.proceed()
                        }
                    } catch (error: Throwable) {
                        if (entered) throw error
                        diagnose("Slider paint preparation", error)
                        chain.proceed()
                    }
                })
                runCatching { ModernHookRuntime.requireModule().deoptimize(method) }
                    .onFailure { diagnose("Seekbar drawing deoptimization", it) }
            }
        base?.declaredMethods?.filter { it.name in setOf("setCornerRadius", "updateCornerRadius", "onSizeChanged") }?.forEach { method ->
            runCatching { ModernHookRuntime.requireModule().deoptimize(method) }
                .onFailure { diagnose("Slider setter deoptimization", it) }
        }
        HotReloadLifecycle.register("qs:slider-corners", detach = {
            synchronized(applied) { applied.keys.toList() }.forEach(::restore)
        })
        HookLog.i(TAG, "Control Center brightness/volume corner adapters installed")
    }

    private fun customRadius(view: View): Float? {
        if (!QsControlCenterCornerTargets.isMainSlider(view.javaClass.name)) return null
        // Never change values cached by constructors before layout is complete.
        if (view.width <= 0 || view.height <= 0) return null
        val dp = QsTileCornerSettings.radiusXposed() ?: return null
        val seek = runCatching { Reflect.callMethod(view, "getSlider") as? View }.getOrNull()
        val width = seek?.width?.takeIf { it > 0 } ?: view.width
        val height = seek?.height?.takeIf { it > 0 } ?: view.height
        return QsTileCornerRules.pixels(dp, view.resources.displayMetrics.density, width, height)
    }

    private fun updateSafely(view: View?, force: Boolean = false) {
        if (view == null || HotReloadLifecycle.isRetiring || nativeRadius.get() == true) return
        if (!QsControlCenterCornerTargets.isMainSlider(view.javaClass.name)) return
        runCatching {
            val radius = customRadius(view)
            val previous = synchronized(applied) { applied[view] }
            if (radius == null) {
                if (previous != null) restore(view)
                return@runCatching
            }
            if (!force && radius == previous) return@runCatching
            synchronized(applied) { applied[view] = radius }
            try { applyVisualRadius(view, radius) }
            catch (error: Throwable) { restore(view); throw error }
            if (reported.add("applied")) HookLog.i(TAG, "Control Center slider background/fill/stroke corners applied")
        }.onFailure { diagnose("Slider corner refresh", it) }
    }

    private fun applyVisualRadius(view: View, radius: Float) {
        val seek = requireNotNull(Reflect.callMethod(view, "getSlider"))
        val previous = nativeRadius.get()
        nativeRadius.set(true)
        try {
            val original = requireNotNull(Reflect.callMethod(view, "getRoundParams", radius))
            val radiusDp = (Reflect.getObjectField(original, "radiusDp") as Number).toFloat()
            // Preserve the current native/theme smoothness, not a global cached object.
            val weight = (Reflect.getObjectField(seek, "mBackgroundRoundCornerWeight") as Number).toFloat()
            val params = Reflect.callMethod(original, "copy", radiusDp, radius, weight, false)
            // Never call geometry setters here. COUI uses its radius fields to
            // calculate track height as well as touch/drag ranges in draw().
            // Only onDraw temporarily changes the two paint-radius fields.
            val listener = Reflect.callMethod(view, "getOnRadiusUpdateListener")
            if (listener != null) Reflect.callMethod(listener, "invoke", radius, params)
            Reflect.callMethod(seek, "updateBaseMixColorDrawableRadius", radius)
            (seek as? View)?.invalidate()
        } finally {
            if (previous == null) nativeRadius.remove() else nativeRadius.set(previous)
        }
    }

    private fun restore(view: View) {
        val previous = nativeRadius.get()
        nativeRadius.set(true)
        try {
            // Recompute the current system/theme radius, not an obsolete cached
            // radius from before a theme, density or orientation change.
            val radius = (Reflect.callMethod(view, "getRadius") as Number).toFloat()
            Reflect.callMethod(view, "setCornerRadius", radius)
            synchronized(applied) { applied.remove(view) }
        } finally {
            if (previous == null) nativeRadius.remove() else nativeRadius.set(previous)
        }
    }
    private fun diagnose(stage: String, error: Throwable) {
        if (reported.add(stage)) HookLog.w(TAG, "$stage unavailable; native behavior retained", error)
    }
}
