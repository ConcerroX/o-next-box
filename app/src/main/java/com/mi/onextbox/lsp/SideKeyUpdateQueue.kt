package com.mi.onextbox.lsp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal data class SideKeyState(val enabled: Boolean, val config: SideKeyConfig)

internal class SideKeyUpdateQueue(
    private val read: () -> SideKeyState,
    private val write: (SideKeyState) -> SideKeyState,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ONextBox-SideKeySettings").apply { isDaemon = true }
    },
) {
    private val submissionLock = Any()

    fun enqueue(transform: (SideKeyState) -> SideKeyState): Deferred<SideKeyState> {
        val result = CompletableDeferred<SideKeyState>()
        synchronized(submissionLock) {
            try {
                executor.execute {
                    runCatching { write(transform(read())) }
                        .onSuccess { result.complete(it) }
                        .onFailure { result.completeExceptionally(it) }
                }
            } catch (error: Throwable) {
                result.completeExceptionally(error)
            }
        }
        return result
    }

    fun replace(state: SideKeyState): SideKeyState {
        val result = synchronized(submissionLock) {
            executor.submit<SideKeyState> { write(state) }
        }
        return try {
            result.get()
        } catch (error: ExecutionException) {
            throw error.cause ?: error
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw error
        }
    }
}
