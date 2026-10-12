package com.mi.onextbox.lsp

import com.mi.onextbox.lsp.compat.HotReloadResourceCoordinator
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BatteryCycleReadStateTest {
    @Test fun everyCompletedOrCanceledReadAllowsAnotherRequest() {
        val state = BatteryCycleReadState()
        val first = requireNotNull(state.begin())
        assertTrue(state.pending)
        assertNull(state.begin())
        assertTrue(state.complete(first))
        assertFalse(state.pending)
        assertFalse(state.complete(first))
        assertNotNull(state.begin())
    }

    @Test fun cleanupDuringBusyReloadVetoDoesNotNeedDetachOrRestore() {
        val coordinator = HotReloadResourceCoordinator { it() }
        val state = BatteryCycleReadState()
        val request = requireNotNull(state.begin())
        coordinator.register("reader", canReload = {
            assertTrue(coordinator.isRetiring)
            assertTrue(state.complete(request))
            false
        }, detach = { fail("A veto must not detach resources") },
            restore = { fail("A veto does not restore resources") })
        runCatching { coordinator.prepare() }.onSuccess { fail("Expected a busy-resource veto") }
        assertFalse(coordinator.isRetiring)
        assertFalse(state.pending)
        assertNotNull(state.begin())
    }

    @Test fun cleanupDuringCaptureFailureAlsoReleasesTheAttachedCard() {
        val coordinator = HotReloadResourceCoordinator { it() }
        val state = BatteryCycleReadState()
        val request = requireNotNull(state.begin())
        coordinator.register("reader", capture = {
            assertTrue(coordinator.isRetiring)
            assertTrue(state.complete(request))
            error("Another resource cannot be captured")
        }, detach = { fail("Failed capture must not detach resources") })
        runCatching { coordinator.prepare() }.onSuccess { fail("Expected capture failure") }
        assertFalse(state.pending)
        assertNotNull(state.begin())
    }

    @Test fun detachedCardAndPreviousRequestCannotReleaseTheReplacementRead() {
        val state = BatteryCycleReadState()
        val old = requireNotNull(state.begin())
        state.invalidate()
        val replacement = requireNotNull(state.begin())
        assertFalse(state.complete(old))
        assertTrue(state.pending)
        assertTrue(state.complete(replacement))
        assertFalse(state.pending)
    }

    @Test fun completionFromAnOldCardCannotChangeTheNewCard() {
        val oldCard = BatteryCycleReadState()
        val newCard = BatteryCycleReadState()
        val oldRequest = requireNotNull(oldCard.begin())
        requireNotNull(newCard.begin())
        oldCard.invalidate()
        assertFalse(oldCard.complete(oldRequest))
        assertTrue(newCard.pending)
    }

    @Test fun hostCompletionClearsOwnershipBeforeRetirementAndViewChecks() {
        val completion = source().substringAfter("fun complete(reading: Reading?, retryAfterRejection: Boolean = false) {")
            .substringBefore("reader.execute {")
        val clear = completion.indexOf("row.readState.complete(generation)")
        assertTrue(clear >= 0)
        assertTrue(completion.indexOf("HotReloadLifecycle.isRetiring") > clear)
        assertTrue(completion.indexOf("rootRef.get()") > clear)
        assertTrue(completion.contains("Looper.myLooper() == Looper.getMainLooper()"))
        assertTrue(completion.contains("main.post(completion)"))
    }

    @Test fun readerEarlyReturnsAndExceptionsStillCompleteTheRequest() {
        val update = source().substringAfter("private fun update(root: ViewGroup) {")
            .substringBefore("private fun append(root: ViewGroup)")
        assertTrue(Regex("finally\\s*\\{\\s*complete\\(result, retryAfterRejection\\)").containsMatchIn(update))
        assertTrue(Regex("catch \\(error: Throwable\\) \\{\\s*complete\\(null\\)").containsMatchIn(update))
    }

    @Test fun canceledReadIsRetriedRegardlessOfRejectionCompletionOrdering() {
        val text = source()
        assertTrue(Regex("resumeAfterRejection = \\{\\s*roots\\.keys\\.toList\\(\\)\\.forEach\\(::update\\)").containsMatchIn(text))
        val update = text.substringAfter("private fun update(root: ViewGroup) {")
            .substringBefore("private fun append(root: ViewGroup)")
        assertTrue(update.contains("if (retryAfterRejection) update(target)"))
        assertTrue(update.indexOf("HotReloadLifecycle.isRetiring) return@Runnable") <
            update.indexOf("if (retryAfterRejection) update(target)"))
    }

    private fun source(): String = listOf(
        File("src/main/java/com/mi/onextbox/lsp/BatteryCycleCountHooker.kt"),
        File("app/src/main/java/com/mi/onextbox/lsp/BatteryCycleCountHooker.kt"),
    ).first { it.isFile }.readText()
}
