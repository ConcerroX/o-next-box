package com.mi.onextbox.ui.settings

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureSettingWriteQueueTest {
    @Test
    fun writesFromSeparateCallersKeepAcceptanceOrder() = runBlocking {
        val executor = Executors.newSingleThreadExecutor()
        val release = CountDownLatch(1)
        try {
            val queue = FeatureSettingWriteQueue(executor)
            val started = CountDownLatch(1)
            val writes = mutableListOf<Boolean>()
            val first = queue.enqueue {
                started.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                writes.add(true)
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val second = queue.enqueue { writes.add(false) }
            release.countDown()
            withTimeout(5_000) {
                first.await()
                second.await()
            }
            assertEquals(listOf(true, false), writes)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun failedSaveDoesNotStopFollowingSave() = runBlocking {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val queue = FeatureSettingWriteQueue(executor)
            val failed = queue.enqueue { error("Failed to persist") }
            var saved = false
            val following = queue.enqueue { saved = true }
            withTimeout(5_000) {
                val failure = runCatching { failed.await() }.exceptionOrNull()
                assertTrue(failure is IllegalStateException)
                following.await()
            }
            assertTrue(saved)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun cancellingPageWaitDoesNotCancelAcceptedSave() = runBlocking {
        val executor = Executors.newSingleThreadExecutor()
        val release = CountDownLatch(1)
        try {
            val queue = FeatureSettingWriteQueue(executor)
            val started = CountDownLatch(1)
            var saved = false
            val pending = queue.enqueue {
                started.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                saved = true
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val pageWait = async(start = CoroutineStart.UNDISPATCHED) { pending.await() }
            pageWait.cancel()
            pageWait.join()
            release.countDown()
            withTimeout(5_000) { pending.await() }
            assertTrue(saved)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }
}
