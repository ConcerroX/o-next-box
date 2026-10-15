package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class CarrierNameFormatScopeTest {
    @Test fun frameworkGettersOutsideCarrierFormatterSeeNoOwner() {
        val scope = CarrierNameFormatScope()
        assertNull(scope.owner())
        scope.during(null) { assertNull(scope.owner()) }
    }

    @Test fun onlyExactNativeFormatterOwnsTheThreadScope() {
        val scope = CarrierNameFormatScope()
        val owner = Any()
        scope.during(owner) { assertSame(owner, scope.owner()) }
        assertNull(scope.owner())
    }

    @Test fun nestedFormattersRestoreCorrectOwner() {
        val scope = CarrierNameFormatScope()
        val first = Any()
        val second = Any()
        scope.during(first) {
            scope.during(second) { assertSame(second, scope.owner()) }
            assertSame(first, scope.owner())
        }
    }

    @Test fun disabledNestedFormatterDoesNotInheritAnAliasScope() {
        val scope = CarrierNameFormatScope()
        val owner = Any()
        scope.during(owner) {
            scope.during(null) { assertNull(scope.owner()) }
            assertSame(owner, scope.owner())
        }
    }

    @Test fun exceptionsCannotLeakAliasScopeToUnrelatedTelephonyConsumers() {
        val scope = CarrierNameFormatScope()
        runCatching { scope.during(Any()) { error("interrupted formatting") } }
        assertNull(scope.owner())
    }

    @Test fun anotherThreadIsUnaffected() {
        val scope = CarrierNameFormatScope()
        val owner = Any()
        scope.during(owner) {
            var other: Any? = owner
            Thread { other = scope.owner() }.apply { start(); join() }
            assertNull(other)
            assertSame(owner, scope.owner())
        }
    }

    @Test fun oneFormatterQueriesEachPhysicalSlotAndSubscriptionOnlyOnce() {
        val scope = CarrierNameFormatScope()
        var reads = 0
        scope.during(Any()) {
            assertEquals("Second", scope.resolve(1, 1) { reads++; "Second" })
            assertEquals("Second", scope.resolve(1, 1) { reads++; "Wrong" })
            assertEquals("First", scope.resolve(0, 2) { reads++; "First" })
            assertEquals("New subscription", scope.resolve(1, 3) { reads++; "New subscription" })
            assertEquals("Other slot", scope.resolve(0, 1) { reads++; "Other slot" })
        }
        assertEquals(4, reads)
    }

    @Test fun unavailableAliasIsAlsoCachedForTheCurrentFormatterOnly() {
        val scope = CarrierNameFormatScope()
        var reads = 0
        scope.during(Any()) {
            assertNull(scope.resolve(1, 1) { reads++; null })
            assertNull(scope.resolve(1, 1) { reads++; "Unexpected" })
        }
        scope.during(Any()) {
            assertEquals("Available now", scope.resolve(1, 1) { reads++; "Available now" })
        }
        assertEquals(2, reads)
    }

    @Test fun appliedNamesAndCacheAreIsolatedAcrossNestedScopes() {
        val scope = CarrierNameFormatScope()
        scope.during(Any()) {
            scope.markApplied(0, "First")
            assertEquals("Second", scope.resolve(1, 1) { "Second" })
            scope.during(Any()) {
                assertNull(scope.applied(0))
                scope.markApplied(0, "Inner")
                assertEquals("Inner", scope.resolve(1, 1) { "Inner" })
            }
            assertEquals("First", scope.applied(0))
            assertNull(scope.applied(1))
            assertEquals("Second", scope.resolve(1, 1) { error("Should use outer cache") })
        }
        assertNull(scope.applied(0))
    }

    @Test fun exceptionsAndDisabledScopesCannotLeakCachedOrAppliedAliases() {
        val scope = CarrierNameFormatScope()
        runCatching {
            scope.during(Any()) {
                scope.markApplied(1, "Second")
                scope.resolve(1, 1) { "Second" }
                scope.during(null) {
                    assertNull(scope.applied(1))
                    assertEquals("Native", scope.resolve(1, 1) { "Native" })
                }
                assertEquals("Second", scope.applied(1))
                error("Aborted formatter")
            }
        }
        assertNull(scope.applied(1))
        assertEquals("Fresh", scope.resolve(1, 1) { "Fresh" })
    }

    @Test fun anotherThreadCannotReadOrChangeAliasCache() {
        val scope = CarrierNameFormatScope()
        scope.during(Any()) {
            scope.markApplied(1, "Second")
            scope.resolve(1, 1) { "Second" }
            var result: String? = null
            var applied: String? = "Unexpected"
            Thread {
                applied = scope.applied(1)
                scope.markApplied(1, "Other")
                result = scope.resolve(1, 1) { "Other" }
            }.apply { start(); join() }
            assertNull(applied)
            assertEquals("Other", result)
            assertEquals("Second", scope.applied(1))
            assertEquals("Second", scope.resolve(1, 1) { "Wrong" })
        }
    }
}
