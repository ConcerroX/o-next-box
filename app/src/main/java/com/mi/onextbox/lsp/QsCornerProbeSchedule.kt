package com.mi.onextbox.lsp

/** Cheap frame gate. Material inspection is event-driven with a low-rate fallback. */
internal class QsCornerProbeSchedule {
    private var dirty = true
    private var visible = false
    private var radius: Int? = null
    private var width = -1
    private var height = -1
    private var background = 0
    private var parent = 0
    private var density = 0f
    private var direction = -1
    private var next = 0L

    fun invalidate() { dirty = true }

    fun shouldProbe(now: Long, shown: Boolean, dp: Int?, w: Int, h: Int,
        bg: Int, owner: Int, scale: Float, layoutDirection: Int): Boolean {
        if (!shown) { visible = false; return false }
        val changed = !visible || radius != dp || width != w || height != h ||
            background != bg || parent != owner || density != scale || direction != layoutDirection
        visible = true
        radius = dp; width = w; height = h; background = bg; parent = owner
        density = scale; direction = layoutDirection
        if (!dirty && !changed && (dp == null || now < next)) return false
        dirty = false
        next = now + 500L
        return true
    }
}
