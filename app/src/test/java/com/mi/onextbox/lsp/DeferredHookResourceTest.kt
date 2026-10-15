package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class DeferredHookResourceTest {
    @Test fun disabledResourceDoesNotLoadAndRepeatedPreparationReusesIt() {
        var loads = 0
        val original = Any()
        val resource = DeferredHookResource { loads++; original }
        assertEquals(0, loads)
        repeat(3) { assertTrue(resource.prepare { throw AssertionError(it) }) }
        assertEquals(1, loads)
        assertSame(original, resource.value())
    }
    @Test fun failedAbiProbeDoesNotRetryOrFloodLogs() {
        var attempts = 0
        var reports = 0
        val resource = DeferredHookResource<Any> { attempts++; error("ABI unavailable") }
        repeat(10) { assertFalse(resource.prepare { reports++ }) }
        assertEquals(1, attempts)
        assertEquals(1, reports)
    }
}
