package com.mi.onextbox.lsp

/** Cheap view state only; no view/drawable references are retained by the observer. */
internal data class QsCornerRefreshState(
    val radiusDp: Int?, val width: Int, val height: Int, val density: Float,
    val backgroundId: Int, val parentId: Int, val shown: Boolean, val direction: Int,
    val content: List<Any?> = emptyList(),
)

internal class QsCornerRefreshGate {
    private var previous: QsCornerRefreshState? = null
    fun changed(next: QsCornerRefreshState): Boolean {
        if (previous == next) return false
        return true
    }
    fun applied(next: QsCornerRefreshState) { previous = next }
    fun invalidate() { previous = null }
}

/** Includes the material identities: native theme rebuilds may keep the same radius. */
internal data class QsSliderMaterialState(
    val radius: Float, val weight: Float, val scale: Float,
    val left: Int, val top: Int, val right: Int, val bottom: Int,
    val baseId: Int, val activeId: Int, val stroke: Boolean, val detail: Boolean,
) {
    fun sameMaterial(other: QsSliderMaterialState): Boolean =
        weight == other.weight && scale == other.scale && baseId == other.baseId && activeId == other.activeId &&
            stroke == other.stroke && detail == other.detail &&
            (right > left && bottom > top) == (other.right > other.left && other.bottom > other.top)
}
