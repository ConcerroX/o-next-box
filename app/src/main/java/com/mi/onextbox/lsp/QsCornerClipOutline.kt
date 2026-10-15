package com.mi.onextbox.lsp

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

/** Framework round-rect outlines remain clip-capable on ROMs with vendor smooth paths. */
internal class QsCornerClipOutline(private val radius: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(0, 0, view.width, view.height, radius)
    }
}
