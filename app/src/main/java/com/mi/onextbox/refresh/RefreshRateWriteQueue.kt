package com.mi.onextbox.refresh

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Accepted writes and preference saves must finish even if the settings page closes. */
internal object RefreshRateWriteQueue {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    init { scope.launch { for (write in writes) write() } }

    fun enqueue(write: suspend () -> Boolean): Deferred<Boolean> {
        val result = CompletableDeferred<Boolean>()
        if (!writes.trySend {
                try { result.complete(write()) }
                catch (error: Exception) { result.completeExceptionally(error) }
            }.isSuccess) result.complete(false)
        return result
    }
}
