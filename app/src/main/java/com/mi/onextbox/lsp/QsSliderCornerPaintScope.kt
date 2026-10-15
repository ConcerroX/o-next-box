package com.mi.onextbox.lsp

/** Enter only at onDraw: the native draw() has already calculated all track/input rectangles. */
internal class QsSliderCornerPaintScope(private val read: (String) -> Float, private val write: (String, Float) -> Unit) {
    fun <T> paint(progress: Float, background: Float, block: () -> T): T {
        val oldProgress = read(PROGRESS)
        val oldBackground = read(BACKGROUND)
        try {
            write(PROGRESS, progress)
            write(BACKGROUND, background)
            return block()
        } finally {
            try { write(PROGRESS, oldProgress) } finally { write(BACKGROUND, oldBackground) }
        }
    }
    companion object {
        const val PROGRESS = "mCurProgressRadius"
        const val BACKGROUND = "mCurBackgroundRadius"
        fun scaled(custom: Float, current: Float, baseline: Float): Float {
            val scale = if (current.isFinite() && baseline.isFinite() && baseline > 0f) current / baseline else 1f
            val result = custom * scale
            return if (result.isFinite()) result.coerceAtLeast(0f) else custom
        }
    }
}
