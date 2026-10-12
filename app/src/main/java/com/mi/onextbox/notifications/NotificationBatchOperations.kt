package com.mi.onextbox.notifications

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NotificationOperationState(
    val busy: Boolean = false,
    val report: NotificationBatchReport? = null,
    val failed: Boolean = false,
    val undo: List<NotificationMutation> = emptyList(),
    val revision: Int = 0,
)

object NotificationBatchOperations {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val mutable = MutableStateFlow(NotificationOperationState())
    val state = mutable.asStateFlow()

    suspend fun loadUndo(context: Context) = lock.withLock {
        mutable.value = mutable.value.copy(undo = NotificationBatchUndoStore.load(context))
    }

    @Synchronized
    fun start(context: Context, plan: List<NotificationMutation>, undoing: Boolean = false) {
        if (mutable.value.busy || plan.isEmpty()) return
        mutable.value = mutable.value.copy(busy = true, failed = false, report = null)
        val appContext = context.applicationContext
        scope.launch {
            lock.withLock {
                try {
                    val report = NotificationBatchClient.apply(appContext, plan)
                    val undo = if (undoing) {
                        val completed = report.results.filter { it.issue == null || it.issue == NotificationBatchIssue.StateChanged || it.issue == NotificationBatchIssue.IdentityChanged }
                            .map { it.mutation.target to it.mutation.kind }.toSet()
                        mutable.value.undo.filterNot { (it.target to it.kind) in completed }
                    } else report.changes.ifEmpty { mutable.value.undo }
                    mutable.value = mutable.value.copy(report = report, undo = undo)
                    NotificationBatchUndoStore.save(appContext, undo)
                } catch (_: Exception) {
                    mutable.value = mutable.value.copy(failed = true)
                } finally {
                    mutable.value = mutable.value.copy(busy = false, revision = mutable.value.revision + 1)
                }
            }
        }
    }
}
