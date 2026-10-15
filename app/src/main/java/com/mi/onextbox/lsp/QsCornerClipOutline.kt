package com.mi.onextbox.lsp

import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.ViewOutlineProvider
import com.mi.onextbox.lsp.compat.ModernReflect as Reflect

/** Copy the native smooth path into a framework outline, rather than a vendor-only outline mode. */
internal class QsCornerClipOutline(
    private val radius: Float, private val weight: Float?, private val loader: ClassLoader,
) : ViewOutlineProvider() {
    private val path = Path()
    private val bounds = RectF()
    private val radii = FloatArray(8) { radius }
    private var width = -1
    private var height = -1
    private var smooth: ((RectF) -> Unit)? = null
    private var resolved = false

    override fun getOutline(view: View, outline: Outline) {
        if (weight == null || radius <= 0f) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
            return
        }
        if (width != view.width || height != view.height) {
            path.rewind()
            bounds.set(0f, 0f, view.width.toFloat(), view.height.toFloat())
            if (!resolved) {
                resolved = true
                smooth = runCatching {
                    val type = Reflect.findClassIfExists("com.oplus.graphics.OplusPathAdapter", loader)
                        ?: error("Native smooth path adapter unavailable")
                    val adapter = type.getConstructor(Path::class.java, Int::class.javaPrimitiveType).newInstance(path, 1)
                    val method = type.getMethod("addSmoothRoundRect", RectF::class.java, FloatArray::class.java,
                        Path.Direction::class.java, Float::class.javaPrimitiveType)
                    val draw: (RectF) -> Unit = { rect -> method.invoke(adapter, rect, radii, Path.Direction.CW, weight) }
                    draw
                }.getOrNull()
            }
            runCatching { smooth?.invoke(bounds) }.onFailure { smooth = null; path.rewind() }
            if (path.isEmpty) path.addRoundRect(bounds, radius, radius, Path.Direction.CW)
            width = view.width
            height = view.height
        }
        // All framework path outlines support clipping on the module's Android 15+ baseline.
        outline.setPath(path)
    }
}
