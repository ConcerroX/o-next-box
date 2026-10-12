package com.mi.onextbox.lsp.compat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotReloadHookIdsTest {
    @Test
    fun idsUseTheVersionedNamespaceAndStartAtZero() {
        val ids = HotReloadHookIds<String>()
        assertEquals("onextbox.reload.v1:0", ids.next("method"))
        assertEquals("onextbox.reload.v1:1", ids.next("method"))
        assertEquals("onextbox.reload.v1:2", ids.next("method"))
    }

    @Test
    fun eachTargetHasItsOwnOrdinalSequence() {
        val ids = HotReloadHookIds<String>()
        assertEquals("onextbox.reload.v1:0", ids.next("first"))
        assertEquals("onextbox.reload.v1:0", ids.next("second"))
        assertEquals("onextbox.reload.v1:1", ids.next("first"))
        assertEquals("onextbox.reload.v1:1", ids.next("second"))
    }

    @Test
    fun aFreshGenerationReproducesTheSameIds() {
        val initial = HotReloadHookIds<String>()
        val replacement = HotReloadHookIds<String>()
        listOf("first", "second", "first", "third", "second", "first").forEach { target ->
            assertEquals(target, initial.next(target), replacement.next(target))
        }
    }

    @Test
    fun unrelatedTargetRegistrationOrderDoesNotChangeExistingIds() {
        val initial = HotReloadHookIds<String>()
        val replacement = HotReloadHookIds<String>()
        val first = initial.next("method")
        val second = initial.next("method")
        replacement.next("new-method")
        assertEquals(first, replacement.next("method"))
        replacement.next("new-method")
        assertEquals(second, replacement.next("method"))
    }

    @Test
    fun distinctTargetIdentitiesDoNotShareAnOrdinalCounter() {
        val ids = HotReloadHookIds<Any>()
        val first = Any()
        val second = Any()
        assertEquals("onextbox.reload.v1:0", ids.next(first))
        assertEquals("onextbox.reload.v1:1", ids.next(first))
        assertEquals("onextbox.reload.v1:0", ids.next(second))
    }

    @Test
    fun equalTargetValuesUseTheSameSequence() {
        val ids = HotReloadHookIds<List<String>>()
        assertEquals("onextbox.reload.v1:0", ids.next(listOf("class", "method")))
        assertEquals("onextbox.reload.v1:1", ids.next(listOf("class", "method")))
    }
}

class HotReloadHookPlanTest {
    @Test
    fun noPreviousHooksRequiresNoReplacementKeys() {
        assertTrue(preservesHookCoverage(emptyList<HotReloadHookKey<String>>(), emptyList()))
        assertTrue(preservesHookCoverage(emptyList(), listOf(key("new"))))
    }

    @Test
    fun exactPreviousKeysAreCoveredRegardlessOfOrder() {
        val first = key("first")
        val second = key("second")
        assertTrue(preservesHookCoverage(listOf(first, second), listOf(second, first)))
    }

    @Test
    fun additionalDistinctKeysAreAllowed() {
        assertTrue(preservesHookCoverage(
            listOf(key("method")),
            listOf(key("method", 1), key("other"), key("method")),
        ))
    }

    @Test
    fun missingPreviousTargetRejectsReplacement() {
        assertFalse(preservesHookCoverage(
            listOf(key("first"), key("second")),
            listOf(key("first"), key("new")),
        ))
        assertFalse(preservesHookCoverage(listOf(key("first")), emptyList()))
    }

    @Test
    fun matchingTargetWithDifferentIdIsNotAReplacement() {
        assertFalse(preservesHookCoverage(listOf(key("method")), listOf(key("method", 1))))
    }

    @Test
    fun multipleHooksOnOneTargetMustPreserveEveryOrdinal() {
        val old = listOf(key("method"), key("method", 1))
        assertFalse(preservesHookCoverage(old, listOf(key("method"))))
        assertFalse(preservesHookCoverage(old, listOf(key("method"), key("method", 2))))
        assertTrue(preservesHookCoverage(old, listOf(key("method", 1), key("method"))))
    }

    @Test
    fun repeatedIdAcrossDifferentTargetsIsValid() {
        val targets = listOf(key("first"), key("second"))
        assertEquals(targets[0].id, targets[1].id)
        assertTrue(preservesHookCoverage(targets, targets))
    }

    @Test
    fun duplicateOldKeysAreRejectedEvenIfNewKeysMatch() {
        val duplicate = key("method")
        assertFalse(preservesHookCoverage(listOf(duplicate, duplicate), listOf(duplicate)))
        assertFalse(preservesHookCoverage(listOf(duplicate, duplicate), listOf(duplicate, duplicate)))
    }

    @Test
    fun duplicateReplacementKeysAreRejectedBeforeAnyHookIsChanged() {
        val duplicate = key("method")
        assertFalse(preservesHookCoverage(listOf(duplicate), listOf(duplicate, duplicate)))
        assertFalse(preservesHookCoverage(emptyList(), listOf(duplicate, duplicate)))
        assertFalse(preservesHookCoverage(
            listOf(key("old")),
            listOf(key("old"), duplicate, duplicate),
        ))
    }

    @Test
    fun equalKeysCannotEvadeDuplicateDetectionWithDifferentInstances() {
        assertFalse(preservesHookCoverage(
            listOf(key("method")),
            listOf(key("method"), key("method")),
        ))
    }

    @Test
    fun distinctTargetIdentitiesAreNotMergedByTheSameId() {
        val firstLoaderTarget = Any()
        val secondLoaderTarget = Any()
        val old = HotReloadHookKey(firstLoaderTarget, "onextbox.reload.v1:0")
        val wrongLoader = HotReloadHookKey(secondLoaderTarget, old.id)
        assertFalse(preservesHookCoverage(listOf(old), listOf(wrongLoader)))
        assertTrue(preservesHookCoverage(listOf(old), listOf(old, wrongLoader)))
    }

    private fun key(target: String, ordinal: Int = 0): HotReloadHookKey<String> =
        HotReloadHookKey(target, "onextbox.reload.v1:$ordinal")
}
