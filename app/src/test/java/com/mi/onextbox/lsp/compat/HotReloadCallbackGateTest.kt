package com.mi.onextbox.lsp.compat

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Test

class HotReloadCallbackGateTest {
    @Test fun pausedGenerationUsesNativeFallbackAndCanResume() {
        val gate = HotReloadCallbackGate()
        assertEquals("hook", gate.invoke({ "native" }) { "hook" })
        assertTrue(gate.pause())
        assertEquals("native", gate.invoke({ "native" }) { error("retired hook ran") })
        gate.resume()
        assertEquals("new", gate.invoke({ "native" }) { "new" })
    }

    @Test fun cannotRetireFromInsideOwnCallback() {
        val gate = HotReloadCallbackGate()
        gate.invoke({ fail() }) { assertFalse(gate.pause()) }
        assertTrue(gate.pause())
    }

    @Test fun busyCallbackRejectsRetirementWithoutLeavingGateClosed() {
        val gate = HotReloadCallbackGate()
        val entered = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val worker = Thread { gate.invoke({ fail() }) { entered.countDown(); finish.await() } }
        worker.start()
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertFalse(gate.pause(5))
            assertEquals(42, gate.invoke({ 0 }) { 42 })
        } finally { finish.countDown(); worker.join(2000) }
        assertTrue(gate.pause())
    }

    @Test fun retirementWaitsForRecursionFromAnAlreadyEnteredInvocation() {
        val gate = HotReloadCallbackGate()
        val entered = CountDownLatch(1)
        val recurse = CountDownLatch(1)
        val nestedRan = AtomicBoolean(false)
        val retired = AtomicBoolean(false)
        val worker = Thread {
            gate.invoke({ fail() }) {
                entered.countDown()
                recurse.await()
                gate.invoke({ fail("nested invocation was skipped") }) { nestedRan.set(true) }
            }
        }
        worker.start()
        assertTrue(entered.await(2, TimeUnit.SECONDS))
        val reloader = Thread { retired.set(gate.pause(2000)) }
        reloader.start()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (!gate.invoke({ true }) { false } && System.nanoTime() < deadline) Thread.yield()
        recurse.countDown()
        worker.join(2000)
        reloader.join(2000)
        assertTrue(nestedRan.get())
        assertTrue(retired.get())
    }
}
