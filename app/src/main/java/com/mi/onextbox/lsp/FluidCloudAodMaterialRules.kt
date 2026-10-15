package com.mi.onextbox.lsp

internal object FluidCloudAodMaterialRules {
    fun progress(value: Float?): Float = value?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f

    fun layer(stacked: Boolean, type: Int, overlay: Boolean): Int = when {
        overlay -> 2
        stacked && type in 1..2 -> type
        else -> 0
    }

    fun layerName(layer: Int): String = when (layer) { 1 -> "first"; 2 -> "second"; else -> "normal" }
    fun backgroundName(layer: Int): String = when (layer) {
        1 -> "notification_panoramic_aod_bg_first_stack"
        2 -> "notification_panoramic_aod_bg_second_stack"
        else -> "notification_panoramic_aod_bg"
    }

    fun alpha(fraction: Float): Int = (progress(fraction) * 255f).toInt()
    fun use(enabled: Boolean, customRow: Boolean, fraction: Float): Boolean = enabled && customRow && progress(fraction) > 0f

    fun supportsShaderMix(name: String?): Boolean =
        name == "BlurMixSingle" || name == "BlurMixMulti" || name == "BlurMixMultiWithShader"

    fun needsEffectUpdate(refresh: Boolean, previous: Float?, fraction: Float): Boolean =
        (refresh || previous != fraction) && (progress(fraction) > 0f || progress(previous) > 0f)

    fun drawMaterialTint(fraction: Float, handled: Boolean): Boolean =
        progress(fraction) == 0f || handled

    /** Never capture an earlier replacement as the native mask when changing renderers. */
    fun <T : Any> originalMask(current: T?, replacement: T?, original: T?): T? =
        if (replacement != null && current === replacement) original else current
}

/** Masks only the current unified row's legacy AOD layer, never another row or nested draw. */
internal class FluidCloudAodDrawScope {
    private val active = ThreadLocal<Any?>()
    fun hidesMask(extension: Any?): Boolean = extension != null && active.get() === extension
    fun <T> duringDraw(extension: Any?, draw: () -> T): T {
        val previous = active.get()
        if (extension == null) active.remove() else active.set(extension)
        return try { draw() } finally {
            if (previous == null) active.remove() else active.set(previous)
        }
    }
}
