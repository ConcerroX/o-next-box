package com.mi.onextbox.ui.settings

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal class FeatureSettingWriteQueue(
    private val executor: Executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "ONextBox-FeatureSettings").apply { isDaemon = true }
    },
) {
    fun enqueue(write: () -> Unit): Deferred<Unit> {
        val result = CompletableDeferred<Unit>()
        try {
            executor.execute {
                try {
                    write()
                    result.complete(Unit)
                } catch (failure: Throwable) {
                    result.completeExceptionally(failure)
                }
            }
        } catch (failure: Exception) {
            result.completeExceptionally(failure)
        }
        return result
    }

    companion object {
        val application by lazy { FeatureSettingWriteQueue() }
    }
}
