package com.mi.onextbox.lsp

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import com.mi.onextbox.lsp.compat.ModernReflect as Reflect
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Uses the same native AOD color interpolation and fallback shape as ordinary notifications. */
internal class FluidCloudAodMaterial(private val loader: ClassLoader, private val report: (String, Throwable) -> Unit) {
    private class State {
        var drawable: WeakReference<Any>? = null
        var origin: Any? = null
        var color: Any? = null
        var mask: Drawable? = null
        var originalMask: WeakReference<Drawable>? = null
        var layer = -1
        var revision = -1
        var shader = false
    }
    private val states = WeakHashMap<View, State>()
    private var api: NativeApi? = null

    fun capture(): Array<View> = states.keys.toTypedArray()
    fun active(view: View): Boolean = states.containsKey(view) && progress(view) > 0f

    fun progress(view: View): Float = runCatching {
        val extension = Reflect.callMethod(view, "getExt") ?: return@runCatching 0f
        val repository = Reflect.getObjectField(extension, "oplusPanoramicAodRepository") ?: return@runCatching 0f
        FluidCloudAodMaterialRules.progress((Reflect.callMethod(repository, "getPanoramicAodProgress") as? Number)?.toFloat())
    }.getOrDefault(0f)

    fun prepare(view: View, proxy: Any?, revision: Int, rendererActive: Boolean, fraction: Float = progress(view)): Boolean {
        if (fraction == 0f) { release(view); return false }
        return runCatching {
            val extension = Reflect.callMethod(view, "getExt") ?: return@runCatching false
            val parent = view.parent
            if (!FluidCloudAodMaterialRules.use(true,
                    parent?.javaClass?.name == "com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow", fraction)) {
                release(view)
                return@runCatching false
            }
            val native = api ?: NativeApi(loader).also { api = it }
            val overlay = Reflect.getObjectField(extension, "oplusNotificationOverlayInteractor")?.let {
                Reflect.callMethod(it, "isOverlayShowing", parent, true) == true
            } ?: false
            val layer = FluidCloudAodMaterialRules.layer(
                ((Reflect.callMethod(extension, "getStackedNotificationMaskColor") as? Number)?.toInt() ?: 0) != 0,
                (Reflect.callMethod(extension, "getStackedType") as? Number)?.toInt() ?: 0, overlay,
            )
            val actual = proxy?.let { Reflect.callMethod(it, "getBlurDrawable", null) }
            val mix = if (actual?.javaClass?.simpleName == "PlatformBlurDrawable") Reflect.callMethod(actual, "getCurrentBlurMixConfig") else null
            val shader = rendererActive && FluidCloudAodMaterialRules.supportsShaderMix(mix?.javaClass?.simpleName)
            // Native read/write helpers return EMPTY for unsupported mixes instead of failing.
            // Those must use the ordinary opaque fallback, not hide a mask without a shader.
            val origin = if (shader) mix else null
            val previous = states[view]
            val state = if (previous != null && previous.drawable?.get() === actual && previous.origin == origin &&
                previous.layer == layer && previous.revision == revision && previous.shader == shader) previous else {
                val next = State().apply {
                    this.layer = layer
                    this.revision = revision
                    this.shader = shader
                    this.origin = origin
                    drawable = actual?.let { WeakReference(it) }
                    originalMask = FluidCloudAodMaterialRules.originalMask(
                        Reflect.getObjectField(extension, "panoramicAodDrawable") as? Drawable,
                        previous?.mask, previous?.originalMask?.get(),
                    )?.let { WeakReference(it) }
                    if (shader) {
                        val data = Reflect.callStaticMethod(native.colors, "access\$readBgAodData", actual, layer)
                            ?: error("Native AOD background data unavailable")
                        val name = FluidCloudAodMaterialRules.layerName(layer)
                        val pair = Reflect.newInstance(native.pair,
                            native.color(view.context, "aod_notification_card_${name}_top_layer"),
                            native.color(view.context, "aod_notification_card_${name}_bottom_layer"))
                        color = Reflect.callStaticMethod(native.colors, "access\$createBgAodColor", data, pair, layer, linkedSetOf<Any>())
                            ?: error("Native AOD interpolation unavailable")
                    } else {
                        mask = (Reflect.newInstance(native.background, view.context) as ColorDrawable).apply {
                            color = native.color(view.context, FluidCloudAodMaterialRules.backgroundName(layer))
                        }
                    }
                }
                if (previous != null) {
                    if (previous.drawable?.get() !== actual || previous.shader != shader) restoreShader(previous)
                    restoreMask(view, extension, previous)
                }
                next
            }
            // Track ownership before mutating native state so a failed write can roll it back.
            states[view] = state
            if (shader) {
                val current = Reflect.callMethod(state.color, "getColor", fraction) ?: error("Native AOD color unavailable")
                Reflect.callMethod(actual, "setAodState", true)
                Reflect.callStaticMethod(native.colors, "writeBgAodData", actual, current)
            } else {
                state.mask?.alpha = FluidCloudAodMaterialRules.alpha(fraction)
                // When there is no unified renderer, the original draw still owns clipping.
                if (!rendererActive) Reflect.callMethod(extension, "setPanoramicAodDrawable", state.mask)
            }
            true
        }.onFailure {
            release(view)
            report("Cannot apply ordinary notification AOD material", it)
        }.getOrDefault(false)
    }

    fun draw(view: View, canvas: Canvas, proxy: Any) {
        val state = states[view] ?: return
        if (state.shader || progress(view) == 0f) return
        val mask = state.mask ?: return
        val actual = Reflect.callMethod(proxy, "getBlurDrawable", null) as? Drawable ?: return
        mask.bounds = actual.bounds
        if (mask.bounds.isEmpty) mask.setBounds(0, 0,
            (Reflect.callMethod(view, "getActualWidth") as Number).toInt(),
            (Reflect.callMethod(view, "getActualHeight") as Number).toInt())
        // The native inverse-round-rect CLEAR path must stay in its own layer.
        val save = canvas.saveLayer(RectF(mask.bounds), null)
        try { mask.draw(canvas) } finally { canvas.restoreToCount(save) }
    }

    fun release(view: View) {
        val state = states.remove(view) ?: return
        runCatching {
            restoreShader(state)
            val extension = Reflect.callMethod(view, "getExt") ?: return@runCatching
            restoreMask(view, extension, state)
        }.onFailure { report("Cannot restore native AOD background", it) }
    }

    fun releaseAll() { states.keys.toList().forEach(::release) }

    private fun restoreMask(view: View, extension: Any, state: State) {
        if (state.mask != null && Reflect.getObjectField(extension, "panoramicAodDrawable") === state.mask) {
            Reflect.callMethod(extension, "setPanoramicAodDrawable", if (progress(view) > 0f) state.originalMask?.get() else null)
        }
    }

    private fun restoreShader(state: State) {
        if (!state.shader) return
        val drawable = state.drawable?.get() ?: return
        Reflect.callMethod(drawable, "setAodState", false)
        val origin = Reflect.callMethod(drawable, "getCurrentBlurMixConfig") ?: return
        Reflect.callMethod(drawable, "changeBlurColor", origin, true)
    }

    private class NativeApi(loader: ClassLoader) {
        val colors = Reflect.findClass("com.oplus.systemui.notification.aod.notification.AodBgViewWrapperKt", loader)
        val background = Reflect.findClass("com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable", loader)
        val pair = Reflect.findClass("kotlin.Pair", loader)
        @Suppress("DiscouragedApi")
        fun color(context: Context, name: String): Int {
            val id = context.resources.getIdentifier(name, "color", "com.android.systemui")
            check(id != 0) { "Missing ordinary AOD material resource: $name" }
            return context.getColor(id)
        }
    }
}
