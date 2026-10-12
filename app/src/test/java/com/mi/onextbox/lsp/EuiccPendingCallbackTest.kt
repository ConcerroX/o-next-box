package com.mi.onextbox.lsp

import com.mi.onextbox.lsp.compat.HotReloadCallbackGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class EuiccPendingCallbackTest {
    @Test fun requestStaysPendingThroughoutCallbackProcessing() {
        val count = AtomicInteger()
        val callback = EuiccPendingCallback(count)
        callback.begin()
        assertEquals("done", callback.complete {
            assertEquals(1, count.get())
            "done"
        })
        assertEquals(0, count.get())
    }

    @Test fun throwingCallbackStillCompletesExactlyOnce() {
        val count = AtomicInteger()
        val callback = EuiccPendingCallback(count)
        callback.begin()
        val result = runCatching { callback.complete { error("parse failed") } }
        assertTrue(result.isFailure)
        callback.cancel()
        callback.complete { Unit }
        assertEquals(0, count.get())
    }

    @Test fun requestFailureCannotDecrementAnAlreadyExecutingCallback() {
        val count = AtomicInteger()
        val callback = EuiccPendingCallback(count)
        callback.begin()
        callback.complete {
            callback.cancel()
            assertEquals(1, count.get())
        }
        assertEquals(0, count.get())
    }

    @Test fun retirementFallbackCompletesTheRequestWithoutProcessingItsPayload() {
        val count = AtomicInteger()
        val callback = EuiccPendingCallback(count)
        val gate = HotReloadCallbackGate()
        callback.begin()
        assertTrue(gate.pause())
        val result = callback.complete {
            gate.invoke(fallback = { "retired" }, action = { error("Old payload must not execute") })
        }
        assertEquals("retired", result)
        assertEquals(0, count.get())
    }

    @Test fun gateCannotRetireWhileCallbackIsPublishing() {
        val count = AtomicInteger()
        val callback = EuiccPendingCallback(count)
        val gate = HotReloadCallbackGate()
        val entered = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val worker = Executors.newSingleThreadExecutor()
        callback.begin()
        try {
            val completion = worker.submit<Boolean> {
                callback.complete {
                    gate.invoke(fallback = { false }) {
                        entered.countDown()
                        check(finish.await(2, TimeUnit.SECONDS))
                        true
                    }
                }
            }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertEquals(1, count.get())
            assertFalse(gate.pause(10))
            assertEquals(1, count.get())
            finish.countDown()
            assertTrue(completion.get(2, TimeUnit.SECONDS))
            assertEquals(0, count.get())
            assertTrue(gate.pause())
        } finally {
            finish.countDown()
            worker.shutdownNow()
        }
    }
}
