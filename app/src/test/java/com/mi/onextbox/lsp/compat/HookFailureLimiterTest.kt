package com.mi.onextbox.lsp.compat

import org.junit.Assert.*
import org.junit.Test

class HookFailureLimiterTest {
    @Test fun firstFailureAndWindowBoundaryAreReported() {
        val limiter = HookFailureLimiter<String>(100)
        assertEquals(0L, limiter.record("a", 0))
        assertNull(limiter.record("a", 1))
        assertNull(limiter.record("a", 99))
        assertEquals(2L, limiter.record("a", 100))
        assertEquals(0L, limiter.record("a", 200))
    }

    @Test fun DifferentKeysDoNotHideEachOther() {
        val limiter = HookFailureLimiter<String>()
        assertEquals(0L, limiter.record("before/a", 0))
        assertEquals(0L, limiter.record("after/a", 0))
        assertNull(limiter.record("before/a", 1))
    }

    @Test fun boundedHistoryEvictsOldestAndClockRollbackDoesNotSilenceErrors() {
        val limiter = HookFailureLimiter<String>(100, 2)
        limiter.record("a", 10)
        limiter.record("b", 10)
        limiter.record("c", 10)
        assertEquals(0L, limiter.record("a", 11))
        assertEquals(0L, limiter.record("a", 1))
    }

    @Test fun concurrentRepeatsAreCountedExactly() {
        val limiter = HookFailureLimiter<String>(100)
        limiter.record("a", 0)
        val workers = List(8) { Thread { repeat(100) { limiter.record("a", 1) } } }
        workers.forEach(Thread::start)
        workers.forEach(Thread::join)
        assertEquals(800L, limiter.record("a", 100))
    }
}
