package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class SoftwareUpdateBootloaderScopeTest {
    @Test fun stockBehaviorIsPreservedOutsideLocalEntryAndWhenDisabled() {
        val scope = SoftwareUpdateBootloaderScope()
        assertFalse(scope.consumeOverride())
        scope.enter(false) { assertFalse(scope.consumeOverride()) }
        assertFalse(scope.consumeOverride())
    }

    @Test fun onlyTheFirstEntryCheckIsOverriddenAndLaterVerificationRemainsNative() {
        val scope = SoftwareUpdateBootloaderScope()
        repeat(2) {
            scope.enter(true) {
                assertTrue(scope.consumeOverride())
                repeat(10) { assertFalse(scope.consumeOverride()) }
            }
            assertFalse(scope.consumeOverride())
        }
    }

    @Test fun nestedCallsRestoreTheOuterBudgetEvenAfterExceptions() {
        val scope = SoftwareUpdateBootloaderScope()
        scope.enter(true) {
            scope.enter(false) { assertFalse(scope.consumeOverride()) }
            assertThrows(IllegalStateException::class.java) {
                scope.enter(true) {
                    assertTrue(scope.consumeOverride())
                    error("native preflight failed")
                }
            }
            assertTrue(scope.consumeOverride())
            scope.enter(true) { assertTrue(scope.consumeOverride()) }
            assertFalse(scope.consumeOverride())
        }
        assertFalse(scope.consumeOverride())
    }

    @Test fun anExceptionalExitNeverLeavesTheSharedGuardOverridden() {
        val scope = SoftwareUpdateBootloaderScope()
        assertThrows(IllegalStateException::class.java) { scope.enter(true) { error("local picker failed") } }
        assertFalse(scope.consumeOverride())
    }

    @Test fun workerThreadsCannotInheritOrConsumeAnotherThreadsBudget() {
        val scope = SoftwareUpdateBootloaderScope()
        scope.enter(true) {
            var inherited: Boolean? = null
            Thread {
                inherited = scope.consumeOverride()
                scope.enter(true) { assertTrue(scope.consumeOverride()) }
            }.also { it.start(); it.join() }
            assertEquals(false, inherited)
            assertTrue(scope.consumeOverride())
            assertFalse(scope.consumeOverride())
        }
    }
}
