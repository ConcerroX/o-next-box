package com.mi.onextbox.refresh

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class RefreshRateTaskRunnerTest {
    @Test fun blockedBinderDoesNotHoldTheCallerOrTheNextOperation() = runBlocking {
        val release = CountDownLatch(1)
        RefreshRateTaskRunner(2).use { runner ->
            try {
                withTimeout(2_000) {
                    assertNull(runner.run(100) {
                        // A synchronous Binder call may ignore interruption entirely.
                        while (release.count != 0L) {
                            try { release.await() } catch (_: InterruptedException) { }
                        }
                        1
                    })
                    assertEquals(2, runner.run(500) { 2 })
                }
            } finally { release.countDown() }
        }
    }

    @Test fun timedOutQueuedWriteNeverExecutesLater() = runBlocking {
        val release = CountDownLatch(1)
        val started = CountDownLatch(1)
        val writes = AtomicInteger()
        RefreshRateTaskRunner(1).use { runner ->
            try {
                val first = async(start = CoroutineStart.UNDISPATCHED) {
                    runner.run(3_000) { started.countDown(); release.await(3, TimeUnit.SECONDS); 1 }
                }
                assertTrue(started.await(1, TimeUnit.SECONDS))
                assertNull(runner.run(100) { writes.incrementAndGet() })
                release.countDown()
                assertEquals(1, first.await())
                assertEquals(2, runner.run(1_000) { 2 })
                assertEquals(0, writes.get())
            } finally { release.countDown() }
        }
    }

    @Test fun exceptionDoesNotPoisonSubsequentRequests() = runBlocking {
        RefreshRateTaskRunner(1).use { runner ->
            val failure = runCatching { runner.run(1_000) { error("service died") } }.exceptionOrNull()
            assertTrue(failure is IllegalStateException)
            assertEquals(7, runner.run(1_000) { 7 })
        }
    }
}
