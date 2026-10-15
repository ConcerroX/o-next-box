package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class QsControlCenterCornerTargetsTest {
    @Test fun separateControlCenterBrightnessAndVolumeLayoutsAreSupported() {
        assertTrue(QsControlCenterCornerTargets.isMainSlider("com.oplus.systemui.plugins.qs.widget.OplusQsToggleSliderLayout"))
    }
    @Test fun combinedControlCenterBrightnessAndVolumeLayoutsAreSupported() {
        assertTrue(QsControlCenterCornerTargets.isMainSlider("com.oplus.systemui.qs.widget.OplusQsToggleSliderLayout"))
    }
    @Test fun detailPopupsAndStandaloneVolumePanelsAreExcluded() {
        for (type in listOf(
            "com.oplus.systemui.qs.slider.OplusQsDetailToggleSliderLayout",
            "com.oplus.systemui.volume.OplusVolumeSeekBar",
            "com.oplus.systemui.qs.widget.OplusToggleSliderView",
            QsControlCenterCornerTargets.BASE_SLIDER,
            QsControlCenterCornerTargets.SEEK_BAR,
        )) assertFalse(type, QsControlCenterCornerTargets.isMainSlider(type))
    }
    @Test fun matchingIsExactNotAnUnboundedClassNameSubstring() {
        assertEquals(2, QsControlCenterCornerTargets.mainSliders.size)
        for (type in QsControlCenterCornerTargets.mainSliders) {
            assertFalse(QsControlCenterCornerTargets.isMainSlider("$type\$Detail"))
            assertFalse(QsControlCenterCornerTargets.isMainSlider("${type}Fake"))
        }
    }
}
