package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class AospBackPanelRulesTest {
    @Test fun onlySingleFingerTouchscreenGesturesAreEligible() {
        assertTrue(AospBackPanelRules.acceptsInput(true, true, 1, true))
        assertFalse(AospBackPanelRules.acceptsInput(false, true, 1, true))
        assertFalse(AospBackPanelRules.acceptsInput(true, false, 1, true))
        assertFalse(AospBackPanelRules.acceptsInput(true, true, 2, true))
        assertFalse(AospBackPanelRules.acceptsInput(true, true, 1, false))
    }

    @Test fun disabledGestureNeverStartsOrHidesStockVisuals() {
        val gesture = AospBackPanelGesture(false)
        assertFalse(gesture.start { fail("Disabled gesture started") })
        assertFalse(gesture.usingNative)
    }

    @Test fun stockVisualIsSuppressedOnlyAfterNativeStartupSucceeded() {
        val gesture = AospBackPanelGesture(true)
        runCatching { gesture.start { error("Unsupported native panel") } }
        assertFalse(gesture.usingNative)
        var starts = 0
        assertTrue(gesture.start { starts++ })
        assertTrue(gesture.start { starts++ })
        assertEquals(1, starts)
        gesture.fail()
        assertFalse(gesture.usingNative)
    }

    @Test fun completionUsesTheStockGestureDecisionNotAnIndependentReturnAction() {
        val gesture = AospBackPanelGesture(true)
        gesture.start { }
        assertEquals(3, gesture.terminalAction(1))
        gesture.finish(2)
        assertEquals(1, gesture.terminalAction(1))
        assertEquals(3, gesture.terminalAction(3))
        gesture.finish(1)
        assertEquals(3, gesture.terminalAction(1))
    }

    @Test fun reloadWaitsForTouchAndNativeAnimationsToSettle() {
        assertFalse(AospBackPanelRules.canReload(1, true, 2000, 0))
        assertFalse(AospBackPanelRules.canReload(0, false, 2000, 0))
        assertFalse(AospBackPanelRules.canReload(0, true, 1699, 1000))
        assertTrue(AospBackPanelRules.canReload(0, true, 1700, 1000))
        assertFalse(AospBackPanelRules.canReload(0, true, 900, 1000))
    }

    interface BackCallback {
        fun triggerBack()
        fun cancelBack()
        fun setTriggerBack(enabled: Boolean)
    }

    @Test fun nativeVisualCallbacksRemainNoOpsWithoutRelyingOnTheHookDispatchGate() {
        val visual = Proxy.newProxyInstance(BackCallback::class.java.classLoader,
            arrayOf(BackCallback::class.java), AospBackPanelVisualCallback) as BackCallback
        visual.triggerBack()
        visual.cancelBack()
        visual.setTriggerBack(true)
        visual.setTriggerBack(false)
        assertSame(AospBackPanelVisualCallback, Proxy.getInvocationHandler(visual))
        assertTrue(visual == visual)
        assertFalse(visual.equals(null))
        assertEquals(System.identityHashCode(visual), visual.hashCode())
        assertEquals("ONextBox-AospBackPanelVisualCallback", visual.toString())
        val results = arrayOfNulls<Throwable>(1)
        Thread {
            try { visual.triggerBack(); visual.cancelBack() } catch (failure: Throwable) { results[0] = failure }
        }.apply { start(); join() }
        assertNull(results[0])
    }
}
