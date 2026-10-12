package com.mi.onextbox.lsp

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SideKeyUpdateQueueTest {
    private val defaults = SideKeyState(false, SideKeyConfig())
    private val scan = SideKeyBinding(SideKeyAction.WeChatScan)
    private val pay = SideKeyBinding(SideKeyAction.AlipayPay)
    private val flashlight = SideKeyBinding(SideKeyAction.Flashlight)

    @Test(timeout = 10_000)
    fun changesFromSeparatePagesMergeAfterTheFirstPageLeaves() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val fixture = Fixture(beforeWrite = { count, _ ->
            if (count == 1) {
                started.countDown()
                assertTrue(release.await(5, TimeUnit.SECONDS))
            }
        })
        try {
            val firstPageSave = fixture.queue.enqueue {
                it.copy(config = it.config.copy(single = scan))
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val secondPageInitialState = fixture.saved.get()
            assertEquals(defaults, secondPageInitialState)
            val secondPageSave = async(Dispatchers.Default) {
                fixture.queue.enqueue { latest ->
                    latest.copy(config = latest.config.copy(long = flashlight))
                }
            }.await()
            release.countDown()
            withTimeout(5_000) {
                firstPageSave.await()
                val saved = secondPageSave.await()
                assertEquals(scan, saved.config.single)
                assertEquals(flashlight, saved.config.long)
                assertEquals(saved, fixture.saved.get())
            }
        } finally {
            release.countDown()
            fixture.close()
        }
    }

    @Test(timeout = 10_000)
    fun rapidChangesToTheSameGestureKeepTheLastSubmittedSelection() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val fixture = Fixture(beforeWrite = { count, _ ->
            if (count == 1) {
                started.countDown()
                assertTrue(release.await(5, TimeUnit.SECONDS))
            }
        })
        try {
            val first = fixture.queue.enqueue { it.copy(config = it.config.copy(single = scan)) }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val second = fixture.queue.enqueue { it.copy(config = it.config.copy(single = pay)) }
            val third = fixture.queue.enqueue { it.copy(config = it.config.copy(single = flashlight)) }
            release.countDown()
            withTimeout(5_000) {
                first.await()
                second.await()
                assertEquals(flashlight, third.await().config.single)
            }
            assertEquals(listOf(scan, pay, flashlight), fixture.writes.map { it.config.single })
            assertEquals(flashlight, fixture.saved.get().config.single)
        } finally {
            release.countDown()
            fixture.close()
        }
    }

    @Test(timeout = 10_000)
    fun cancellingThePagesWaitDoesNotCancelItsAcceptedUpdate() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val fixture = Fixture(beforeWrite = { count, _ ->
            if (count == 1) {
                started.countDown()
                assertTrue(release.await(5, TimeUnit.SECONDS))
            }
        })
        try {
            val result = fixture.queue.enqueue { it.copy(enabled = true) }
            val page = launch(start = CoroutineStart.UNDISPATCHED) { result.await() }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            page.cancelAndJoin()
            assertFalse(result.isCancelled)
            val later = fixture.queue.enqueue { it.copy(config = it.config.copy(single = scan)) }
            release.countDown()
            withTimeout(5_000) {
                assertTrue(result.await().enabled)
                assertTrue(later.await().enabled)
                assertEquals(scan, fixture.saved.get().config.single)
            }
        } finally {
            release.countDown()
            fixture.close()
        }
    }

    @Test(timeout = 10_000)
    fun resetIsOrderedAfterExistingWritesAndBeforeLaterUserChanges() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val resetQueued = CountDownLatch(1)
        val submitted = AtomicInteger()
        val executor = object : ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue(),
        ) {
            override fun execute(command: Runnable) {
                super.execute(command)
                if (submitted.incrementAndGet() == 3) resetQueued.countDown()
            }
        }
        val fixture = Fixture(executor = executor, beforeWrite = { count, _ ->
            if (count == 1) {
                started.countDown()
                assertTrue(release.await(5, TimeUnit.SECONDS))
            }
        })
        try {
            val first = fixture.queue.enqueue { it.copy(config = it.config.copy(single = scan)) }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val second = fixture.queue.enqueue { it.copy(config = it.config.copy(long = flashlight)) }
            val reset = async(Dispatchers.Default) { fixture.queue.replace(defaults) }
            assertTrue(resetQueued.await(5, TimeUnit.SECONDS))
            val afterReset = fixture.queue.enqueue { it.copy(enabled = true) }
            release.countDown()
            withTimeout(5_000) {
                first.await()
                second.await()
                assertEquals(defaults, reset.await())
                assertEquals(defaults.copy(enabled = true), afterReset.await())
            }
            assertEquals(defaults, fixture.writes[2])
            assertEquals(defaults.copy(enabled = true), fixture.saved.get())
        } finally {
            release.countDown()
            fixture.close()
        }
    }

    @Test(timeout = 10_000)
    fun failedSaveDoesNotStopTheQueueOrBecomeTheNextUpdatesBase() = runBlocking {
        Fixture(beforeWrite = { count, _ ->
            if (count == 1) error("Write rejected")
        }).use { fixture ->
            val failed = fixture.queue.enqueue { it.copy(config = it.config.copy(single = scan)) }
            val next = fixture.queue.enqueue { it.copy(config = it.config.copy(long = flashlight)) }
            withTimeout(5_000) {
                assertTrue(runCatching { failed.await() }.exceptionOrNull() is IllegalStateException)
                assertEquals(defaults.config.single, next.await().config.single)
                assertEquals(flashlight, fixture.saved.get().config.long)
            }
        }
    }

    private inner class Fixture(
        private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
        beforeWrite: (Int, SideKeyState) -> Unit = { _, _ -> },
    ) : Closeable {
        val saved = AtomicReference(defaults)
        val writes = CopyOnWriteArrayList<SideKeyState>()
        private val writeCount = AtomicInteger()
        val queue = SideKeyUpdateQueue(
            read = saved::get,
            write = { value ->
                beforeWrite(writeCount.incrementAndGet(), value)
                saved.set(value)
                writes.add(value)
                value
            },
            executor = executor,
        )

        override fun close() {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }
}
