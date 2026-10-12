package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class AodAllDaySupportTest {
    private class Controller {
        @JvmField var isSupportPanoramicAllDay = false
        @JvmField var isSupportPanoramicAllDayByPanelFeature = false
    }

    @Test fun reappliesAfterNativeInitializationOverwritesTheSameInstance() {
        val controller = Controller()
        val support = AodAllDaySupport(Controller::class.java) { fail(it) }
        repeat(3) {
            controller.isSupportPanoramicAllDay = false
            controller.isSupportPanoramicAllDayByPanelFeature = false
            support.apply(controller, true)
            assertTrue(controller.isSupportPanoramicAllDay)
            assertTrue(controller.isSupportPanoramicAllDayByPanelFeature)
        }
    }

    @Test fun disabledHookLeavesNativeStateUntouched() {
        val controller = Controller()
        val support = AodAllDaySupport(Controller::class.java) { fail(it) }
        support.apply(controller, false)
        assertFalse(controller.isSupportPanoramicAllDay)
        controller.isSupportPanoramicAllDay = true
        support.apply(controller, false)
        assertTrue(controller.isSupportPanoramicAllDay)
        assertFalse(controller.isSupportPanoramicAllDayByPanelFeature)
        support.apply(null, true)
    }

    @Test fun incompatibleFieldsAreReportedWithoutCrashing() {
        val messages = mutableListOf<String>()
        val support = AodAllDaySupport(Any::class.java, messages::add)
        support.apply(Any(), true)
        assertEquals(2, messages.size)
    }
}
