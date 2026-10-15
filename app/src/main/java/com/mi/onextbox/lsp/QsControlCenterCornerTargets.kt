package com.mi.onextbox.lsp

internal object QsControlCenterCornerTargets {
    const val BASE_SLIDER = "com.oplus.systemui.qs.base.seek.OplusQsBaseToggleSliderLayout"
    const val SEEK_BAR = "com.oplus.systemui.qs.base.seek.OplusQsVerticalSeekBar"
    val mainSliders = setOf(
        "com.oplus.systemui.plugins.qs.widget.OplusQsToggleSliderLayout",
        "com.oplus.systemui.qs.widget.OplusQsToggleSliderLayout",
    )
    // Exact types deliberately exclude the detail popup and standalone volume panel.
    fun isMainSlider(className: String) = className in mainSliders
}
