package com.mi.onextbox.lsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PhoneManagerCountdownRulesTest {
    @Test
    fun disabledLeavesTheOriginalCountdownUntouched() {
        val calls = mutableListOf<String>()
        val result = PhoneManagerCountdownRules.tick(
            enabled = false,
            complete = { calls += "complete" },
            cancel = { calls += "cancel" },
            failure = { calls += "failure" },
            original = { calls += "tick"; "stock" },
        )
        assertEquals("stock", result)
        assertEquals(listOf("tick"), calls)
    }

    @Test
    fun enabledCompletesTheButtonStateBeforeCancellingTheTimer() {
        val calls = mutableListOf<String>()
        val result = PhoneManagerCountdownRules.tick(
            enabled = true,
            complete = { calls += "complete" },
            cancel = { calls += "cancel" },
            failure = { calls += "failure" },
            original = { calls += "tick" },
        )
        assertNull(result)
        assertEquals(listOf("complete", "cancel"), calls)
    }

    @Test
    fun failedNativeCompletionKeepsTheTimerRunning() {
        val calls = mutableListOf<String>()
        val error = IllegalStateException("unavailable button")
        var reported: Throwable? = null
        val result = PhoneManagerCountdownRules.tick(
            enabled = true,
            complete = { calls += "complete"; throw error },
            cancel = { calls += "cancel" },
            failure = { calls += "failure"; reported = it },
            original = { calls += "tick"; "stock" },
        )
        assertSame(error, reported)
        assertEquals("stock", result)
        assertEquals(listOf("complete", "failure", "tick"), calls)
    }

    @Test
    fun eachNewTickUsesTheCurrentSwitchValue() {
        val calls = mutableListOf<String>()
        for (enabled in listOf(false, true, false)) {
            PhoneManagerCountdownRules.tick(
                enabled = enabled,
                complete = { calls += "complete" },
                cancel = { calls += "cancel" },
                failure = { calls += "failure" },
                original = { calls += "tick" },
            )
        }
        assertEquals(listOf("tick", "complete", "cancel", "tick"), calls)
    }
}
