package com.mi.onextbox.lsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherMemoryStateTest {
    private class NativeMemory(@JvmField var allowed: Boolean, @JvmField var detail: Boolean) {
        var userSwitch = false
        fun refresh() { detail = allowed && userSwitch }
    }

    private fun state() = LauncherMemoryState(
        NativeMemory::class.java.getDeclaredField("allowed"),
        NativeMemory::class.java.getDeclaredField("detail"),
    )

    @Test fun forcingExistingDisabledManagerEnablesBothNativeFields() {
        val owner = NativeMemory(false, false)
        val state = state()
        state.force(owner)
        assertTrue(owner.allowed)
        assertTrue(owner.detail)
        assertEquals(1, state.export().size)
    }

    @Test fun repeatedRefreshesNeverOverwriteTheOriginalRestriction() {
        val owner = NativeMemory(false, false)
        val state = state()
        repeat(4) {
            state.force(owner)
            owner.refresh()
            state.force(owner)
        }
        assertTrue(state.restoreAllowed(owner))
        owner.refresh()
        assertFalse(owner.allowed)
        assertFalse(owner.detail)
        assertFalse(state.restoreAllowed(owner))
        assertTrue(state.export().isEmpty())
    }

    @Test fun disablingRetainsAnOriginallyAllowedManagerAndTheCurrentUserSwitch() {
        val owner = NativeMemory(true, false)
        val state = state()
        state.force(owner)
        owner.userSwitch = true
        assertTrue(state.restoreAllowed(owner))
        owner.refresh()
        assertTrue(owner.allowed)
        assertTrue(owner.detail)
    }

    @Test fun disablingHonorsAUserSwitchTurnedOffWhileTheOverrideWasActive() {
        val owner = NativeMemory(true, true).apply { userSwitch = true }
        val state = state()
        state.force(owner)
        owner.userSwitch = false
        state.restoreAllowed(owner)
        owner.refresh()
        assertTrue(owner.allowed)
        assertFalse(owner.detail)
    }

    @Test fun reloadTransfersOnlyHostObjectsAndPreservesTheTrueOriginalAcrossGenerations() {
        val owner = NativeMemory(false, false)
        val first = state()
        first.force(owner)
        val saved = first.export()
        assertTrue(saved.all { it.size == 2 && it[0] === owner && it[1] is Boolean })
        first.restoreAllowed(owner)
        owner.refresh()
        val second = state()
        second.import(saved)
        second.force(owner)
        val next = second.export()
        second.restoreAllowed(owner)
        owner.refresh()
        val third = state()
        third.import(next)
        third.force(owner)
        third.restoreAllowed(owner)
        owner.refresh()
        assertFalse(owner.allowed)
        assertFalse(owner.detail)
    }

    @Test fun reloadWhileTheSwitchIsOffDoesNotLeaveForcedFieldsBehind() {
        val owner = NativeMemory(false, false)
        val first = state()
        first.force(owner)
        val saved = first.export()
        first.restoreAllowed(owner)
        owner.refresh()
        val replacement = state()
        replacement.import(saved)
        replacement.restoreAllowed(owner)
        owner.refresh()
        assertFalse(owner.allowed)
        assertFalse(owner.detail)
        assertTrue(replacement.export().isEmpty())
    }

    @Test fun managersWithDifferentNativeRestrictionsAreRestoredIndependently() {
        val first = NativeMemory(false, false)
        val second = NativeMemory(true, false).apply { userSwitch = true }
        val state = state()
        state.force(first)
        state.force(second)
        state.restoreAllowed(first)
        first.refresh()
        assertFalse(first.allowed)
        assertTrue(second.allowed)
        assertEquals(1, state.export().size)
        state.restoreAllowed(second)
        second.refresh()
        assertTrue(second.detail)
    }

    @Test fun malformedOrForeignImportedRowsAreIgnored() {
        val state = state()
        state.import(arrayOf(null, "row", arrayOf(Any(), false), arrayOf(NativeMemory(false, false), "false")))
        assertTrue(state.export().isEmpty())
    }

    @Test fun importedStateCannotReplaceAnAlreadyCapturedNativeOriginal() {
        val owner = NativeMemory(false, false)
        val state = state()
        state.force(owner)
        state.import(arrayOf(arrayOf(owner, true)))
        state.restoreAllowed(owner)
        assertFalse(owner.allowed)
    }
}
