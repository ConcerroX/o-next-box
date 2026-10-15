package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class FluidCloudAodDrawScopeTest {
    @Test fun onlyTargetExtensionMaskIsHiddenDuringItsDraw() {
        val scope = FluidCloudAodDrawScope()
        val target = Any()
        assertFalse(scope.hidesMask(target))
        scope.duringDraw(target) {
            assertTrue(scope.hidesMask(target))
            assertFalse(scope.hidesMask(Any()))
            assertFalse(scope.hidesMask(null))
        }
        assertFalse(scope.hidesMask(target))
    }

    @Test fun equalButDifferentExtensionCannotLoseItsNativeMask() {
        val scope = FluidCloudAodDrawScope()
        val target = String(charArrayOf('a'))
        val other = String(charArrayOf('a'))
        assertEquals(target, other)
        assertNotSame(target, other)
        scope.duringDraw(target) {
            assertFalse(scope.hidesMask(other))
            assertTrue(scope.hidesMask(target))
        }
    }

    @Test fun nestedOrdinaryDrawShadowsAndRestoresOuterTarget() {
        val scope = FluidCloudAodDrawScope()
        val target = Any()
        scope.duringDraw(target) {
            scope.duringDraw(null) { assertFalse(scope.hidesMask(target)) }
            assertTrue(scope.hidesMask(target))
        }
    }

    @Test fun nestedCustomDrawDoesNotHideOuterMask() {
        val scope = FluidCloudAodDrawScope()
        val outer = Any()
        val inner = Any()
        scope.duringDraw(outer) {
            scope.duringDraw(inner) {
                assertFalse(scope.hidesMask(outer))
                assertTrue(scope.hidesMask(inner))
            }
            assertTrue(scope.hidesMask(outer))
            assertFalse(scope.hidesMask(inner))
        }
    }

    @Test fun interruptedNestedDrawRestoresScopeAndDoesNotLeak() {
        val scope = FluidCloudAodDrawScope()
        val outer = Any()
        val inner = Any()
        scope.duringDraw(outer) {
            runCatching { scope.duringDraw(inner) { error("interrupted draw") } }
            assertTrue(scope.hidesMask(outer))
            assertFalse(scope.hidesMask(inner))
        }
        runCatching { scope.duringDraw(outer) { error("interrupted draw") } }
        assertFalse(scope.hidesMask(outer))
    }

    @Test fun drawingAnotherThreadDoesNotHideItsAodMask() {
        val scope = FluidCloudAodDrawScope()
        val target = Any()
        scope.duringDraw(target) {
            var hidden = true
            Thread { hidden = scope.hidesMask(target) }.apply { start(); join() }
            assertFalse(hidden)
            assertTrue(scope.hidesMask(target))
        }
    }
}
