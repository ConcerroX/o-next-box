package com.mi.onextbox.lsp

import com.mi.onextbox.lsp.compat.HotReloadCallbackGate
import com.mi.onextbox.lsp.compat.HotReloadResourceCoordinator
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAssistantReloadTasksTest {
    private class MainQueue {
        val pending = ArrayDeque<Pair<Runnable, Long>>()
        fun post(task: Runnable, delay: Long): Boolean {
            pending.addLast(task to delay)
            return true
        }
        fun remove(task: Runnable) { pending.removeAll { it.first === task } }
        fun runNext() { pending.removeFirst().first.run() }
    }

    @Test fun claimedCompletionIsRetainedAcrossPauseAndDeliveredExactlyOnce() {
        val main = MainQueue()
        val gate = HotReloadCallbackGate()
        val tasks = NativeAssistantReloadTasks(main::post, main::remove) { action, deferred ->
            gate.invoke(deferred, action)
        }
        var delivered: Boolean? = null
        tasks.post { delivered = true }
        assertTrue(gate.pause())
        main.runNext()
        assertEquals(null, delivered)
        assertFalse(tasks.isIdle())
        gate.resume()
        tasks.resumeDeferred()
        tasks.resumeDeferred()
        assertEquals(1, main.pending.size)
        main.runNext()
        assertEquals(true, delivered)
        assertTrue(tasks.isIdle())
    }

    @Test fun bindingPollResumesWithItsOriginalDeadline() {
        val main = MainQueue()
        val gate = HotReloadCallbackGate()
        val tasks = NativeAssistantReloadTasks(main::post, main::remove) { action, deferred ->
            gate.invoke(deferred, action)
        }
        var now = 100L
        val deadline = 200L
        var timedOut = false
        var polls = 0
        lateinit var poll: Runnable
        poll = Runnable {
            polls++
            if (now >= deadline) timedOut = true else tasks.post(poll, 100L)
        }
        tasks.post(poll)
        main.runNext()
        assertEquals(100L, main.pending.first().second)
        assertTrue(gate.pause())
        main.runNext()
        now = 250L
        gate.resume()
        tasks.resumeDeferred()
        main.runNext()
        assertEquals(2, polls)
        assertTrue(timedOut)
        assertTrue(tasks.isIdle())
    }

    @Test fun canceledDeferredTimeoutIsNotResurrected() {
        val main = MainQueue()
        val gate = HotReloadCallbackGate()
        val tasks = NativeAssistantReloadTasks(main::post, main::remove) { action, deferred ->
            gate.invoke(deferred, action)
        }
        val timeout = Runnable { error("Canceled timeout must not run") }
        tasks.post(timeout, 5_000L)
        assertTrue(gate.pause())
        main.runNext()
        tasks.remove(timeout)
        gate.resume()
        tasks.resumeDeferred()
        assertTrue(tasks.isIdle())
        assertTrue(main.pending.isEmpty())
    }

    @Test fun coordinatorVetoResumesDeferredMainWorkWithoutRecreatingResources() {
        val main = MainQueue()
        val coordinator = HotReloadResourceCoordinator { it() }
        val tasks = NativeAssistantReloadTasks(main::post, main::remove) { action, deferred ->
            coordinator.withHook(deferred, action)
        }
        var completed = false
        var detached = false
        coordinator.register("assistant", canReload = {
            main.runNext()
            tasks.isIdle()
        }, detach = { detached = true }, resumeAfterRejection = tasks::resumeDeferred)
        tasks.post { completed = true }
        assertTrue(runCatching { coordinator.prepare() }.isFailure)
        assertFalse(detached)
        assertFalse(completed)
        assertEquals(1, main.pending.size)
        main.runNext()
        assertTrue(completed)
        assertTrue(tasks.isIdle())
    }

    @Test fun actionAndDispatchFailureDoNotLeavePhantomPendingWork() {
        val main = MainQueue()
        val tasks = NativeAssistantReloadTasks(main::post, main::remove) { action, _ -> action() }
        tasks.post { error("Native invocation failed") }
        assertTrue(runCatching { main.runNext() }.isFailure)
        assertTrue(tasks.isIdle())
        val stopped = NativeAssistantReloadTasks({ _, _ -> false }, {}) { action, _ -> action() }
        assertTrue(runCatching { stopped.post {} }.isFailure)
        assertTrue(stopped.isIdle())
    }

    @Test fun nativeOwnerTracksQueuedWorkAndRetainsResultsDuringPreparation() {
        val text = source("NativeAssistantSessionRecovery.kt")
        assertTrue(text.contains("outstandingCallbacks.get() == 0 && tasks.isIdle()"))
        assertTrue(text.contains("resumeAfterRejection = { tasks.resumeDeferred() }"))
        assertTrue(text.contains("try { complete(it) } finally { releaseCallback() }"))
        assertTrue(text.contains("tasks.post(Runnable { waitForBinding(deadline) }, BIND_POLL_MS)"))
        assertTrue(text.contains("tasks.remove(timeout)"))
        assertFalse(text.contains("handler.post {"))
    }

    @Test fun rejectedMediaReloadRearmsTheExistingDeduplicatedPollOnly() {
        val text = source("SmallWindowPolicyHooker.kt")
        assertTrue(text.contains("resumeAfterRejection = { schedule() }"))
        val schedule = text.substringAfter("fun schedule() {").substringBefore("HotReloadLifecycle.register")
        assertTrue(schedule.contains("it.removeCallbacks(reconcile); it.post(reconcile)"))
        assertFalse(schedule.contains("startWorker()"))
        assertFalse(schedule.contains("owned.clear()"))
    }

    private fun source(name: String): String = listOf(
        File("src/main/java/com/mi/onextbox/lsp/$name"),
        File("app/src/main/java/com/mi/onextbox/lsp/$name"),
    ).first { it.isFile }.readText()
}
