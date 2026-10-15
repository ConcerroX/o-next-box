package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class FaceTapNativeHandoffContractTest {
    private fun source() = listOf(File("src/main/java/com/mi/onextbox/lsp/FaceTapUnlockHooker.kt"),
        File("app/src/main/java/com/mi/onextbox/lsp/FaceTapUnlockHooker.kt")).first { it.isFile }.readText()

    @Test fun pointerHooksUseTheActualAuthenticationCallbackWithoutChangingStockEvents() {
        val hooks = source().substringAfter("private fun installNativePointerHooks(")
            .substringBefore("private fun captureIcon(")
        assertTrue(hooks.contains("mFingerprintAuthenticationCallback"))
        assertTrue(hooks.contains("\"onUdfpsPointerDown\""))
        assertTrue(hooks.contains("\"onUdfpsPointerUp\""))
        assertTrue(hooks.contains("!== eventCallback"))
        assertTrue(hooks.contains("if (!pointerHookMethods.containsAll(requiredMethods)) return@before"))
        assertTrue(hooks.contains("val at = SystemClock.uptimeMillis()"))
        assertTrue(hooks.contains("Native pointer handoff unavailable; window touch retained"))
        assertFalse(hooks.contains("param.result ="))
    }

    @Test fun pendingHandoffIsPolledAndNeverReplaysFingerprintFeedback() {
        val ui = source()
        val reconcile = ui.substringAfter("private fun reconcile(").substringBefore("private fun showButton(")
        assertTrue(reconcile.contains("nativeHandoff.claim(now, authenticatedAt, contactState)"))
        assertTrue(reconcile.contains("requestDismiss(immediate = true, animate = false)"))
        assertTrue(reconcile.contains("FaceTapNativeHandoff.POLL_MS"))
        assertTrue(reconcile.contains("reasons="))
        val dismiss = ui.substringAfter("private fun requestDismiss(").substringBefore("private fun clearSession(")
        assertTrue(dismiss.contains("val state = liveState() ?: return"))
        assertTrue(dismiss.contains("FaceTapUnlockRules.eligible(state)"))
        assertTrue(dismiss.contains("FaceTapFeedbackRules.mayCommit("))
        assertTrue(dismiss.contains("Reflect.callMethod(mediator, \"dismiss\", null, null)"))
        assertTrue(dismiss.contains("nativeHandoff.reset()"))
    }

    @Test fun securityRestrictionsRemainHardWhileDisplayFlagsOnlyDelayTheHandoff() {
        val ui = source()
        val hard = ui.substringAfter("val hardBlocks =").substringBefore("val displayBlocks =")
        for (gate in listOf("mGoingToSleep", "mSwitchingUser", "mOccluded", "mPrimaryBouncerShowing",
            "isSimPinSecure", "isEncryptedOrLockdown", "isBiometricPromptShowing", "showForEnroll")) {
            assertTrue(gate, hard.contains(gate))
        }
        assertTrue(ui.contains("\"hostHidden\" to !host.isShown"))
        assertTrue(ui.contains("blocked = hardBlocked || displayBlocks.values.any { it }"))
        assertTrue(ui.contains("handoffBlocked = hardBlocked"))
        assertTrue(ui.contains("nativeHandoff.windowCanceled(liveState(forNativeContact = true))"))
        assertTrue(ui.contains("MotionEvent.ACTION_UP -> nativeHandoff.touchReleased(event.eventTime)"))
    }

    @Test fun handoffCannotSurviveLifecycleOrHotReloadBoundaries() {
        val ui = source()
        assertTrue(ui.contains("!nativeHandoff.pending"))
        val clearing = ui.substringAfter("private fun clearSession(").substringBefore("private fun removeButton(")
        assertTrue(clearing.contains("authenticatedAt = 0L"))
        assertTrue(clearing.contains("nativeHandoff.reset()"))
        val resume = ui.substringAfter("private fun resumeAfterRejectedReload(").substringBefore("private fun reconcile(")
        assertTrue(resume.contains("nativeHandoff.invalidate()"))
        assertTrue(ui.contains("private fun observeNativeHandoffTouch("))
        assertTrue(ui.contains("event.pointerCount != 1 || touchObscured(event)"))
    }
}
