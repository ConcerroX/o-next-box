package com.mi.onextbox.refresh

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** A coroutine timeout alone cannot interrupt a synchronous Binder or shell call. */
internal class RefreshRateTaskRunner(workers: Int = 3) : AutoCloseable {
    private val executor = ThreadPoolExecutor(workers, workers, 30, TimeUnit.SECONDS,
        ArrayBlockingQueue(8), { work -> Thread(work, "ONextBox-RefreshRateIO").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy()).apply { allowCoreThreadTimeOut(true) }

    suspend fun <T> run(timeoutMs: Long, operation: () -> T): T? = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { continuation ->
            val task = FutureTask {
                try { continuation.resumeWith(Result.success(operation())) }
                catch (error: Throwable) { continuation.resumeWith(Result.failure(error)) }
            }
            continuation.invokeOnCancellation {
                task.cancel(true)
                executor.remove(task)
            }
            try { executor.execute(task) }
            catch (error: RuntimeException) { continuation.resumeWith(Result.failure(error)) }
        }
    }

    override fun close() { executor.shutdownNow() }
}
