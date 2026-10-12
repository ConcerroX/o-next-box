package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class LauncherGridApplyRequestTest {
    private val grid = LauncherGrid(5, 8)
    private fun request(at: Long = 100) = LauncherGridApplyRequest.create(grid, at)

    @Test fun explicitRequestsAreUniqueAndRoundTrip() {
        val first = request()
        assertEquals(first, LauncherGridApplyRequest.parse(first.encode()))
        assertNotEquals(first.token, request().token)
    }

    @Test fun malformedRequestsAndOutOfRangeGridsCannotApply() {
        val token = request().token
        for (value in listOf(null, "", "$token|1|5", "$token|1|5|8|1", "not-a-uuid|1|5|8",
            "$token|-1|5|8", "$token|9223372036854775808|5|8", "$token|1|2|8", "$token|1|6|11")) {
            assertNull(value, LauncherGridApplyRequest.parse(value))
        }
    }

    @Test fun onlyEnabledMatchingRecentRequestsCanApply() {
        val request = request(100)
        val enabled = LauncherGridState(true, grid)
        assertTrue(request.isCurrent(100, enabled))
        assertTrue(request.isCurrent(120099, enabled))
        assertFalse(request.isCurrent(120100, enabled))
        assertFalse(request.isCurrent(99, enabled))
        assertFalse(request.isCurrent(101, enabled.copy(enabled = false)))
        assertFalse(request.isCurrent(101, enabled.copy(grid = LauncherGrid(4, 7))))
    }

    @Test fun continuousAdjustmentsCoalesceToTheLatestRequest() {
        val queue = LauncherGridApplyQueue()
        val first = request()
        val last = request()
        queue.offer(first)
        queue.offer(last)
        assertEquals(last, queue.start())
        assertNull(queue.pending)
        assertEquals(last, queue.running)
        assertEquals(last.token, queue.handled)
    }

    @Test fun runningNativeMigrationCannotBeStartedTwiceAndLatestChangeWaits() {
        val queue = LauncherGridApplyQueue()
        val first = request()
        val second = request()
        queue.offer(first)
        assertEquals(first, queue.start())
        queue.offer(second)
        assertNull(queue.start())
        assertEquals(second, queue.pending)
        queue.finish()
        assertEquals(second, queue.start())
    }

    @Test fun switchingOffCancelsPendingChangesButDoesNotInterruptNativeMigration() {
        val queue = LauncherGridApplyQueue()
        val first = request()
        queue.offer(first)
        queue.start()
        queue.offer(request())
        queue.offer(null)
        assertNull(queue.pending)
        assertEquals(first, queue.running)
        queue.finish()
        assertNull(queue.running)
    }

    @Test fun handledAndRunningRequestsAreNotReplayed() {
        val queue = LauncherGridApplyQueue()
        val request = request()
        queue.offer(request)
        queue.start()
        queue.offer(request)
        assertNull(queue.pending)
        queue.finish()
        queue.offer(request)
        assertNull(queue.pending)
    }

    @Test fun hotReloadOnlyTransfersPrimitiveStateAndKeepsIdempotency() {
        val queue = LauncherGridApplyQueue()
        val handled = request()
        queue.offer(handled)
        queue.consume()
        val pending = request()
        queue.offer(pending)
        val snapshot = queue.capture()
        assertEquals(pending.encode(), snapshot[0])
        assertEquals(handled.token, snapshot[1])
        val replacement = LauncherGridApplyQueue()
        replacement.restore(snapshot)
        assertNull(replacement.running)
        assertEquals(pending, replacement.pending)
        assertEquals(handled.token, replacement.handled)
        replacement.offer(handled)
        assertNull(replacement.pending)
    }

    @Test fun invalidRestoredStateDoesNotCreateWork() {
        val queue = LauncherGridApplyQueue()
        queue.restore(arrayOf(123, null))
        assertNull(queue.pending)
        assertNull(queue.running)
    }
}
