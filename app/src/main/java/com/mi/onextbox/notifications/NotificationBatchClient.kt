package com.mi.onextbox.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object NotificationBatchClient {
    private val connectionLock = Mutex()
    private val operationLock = Mutex()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var service: INotificationBatchController? = null
    @Volatile private var pending: CompletableDeferred<INotificationBatchController>? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val connected = INotificationBatchController.Stub.asInterface(binder)
            if (connected == null) onNullBinding(name) else {
                service = connected
                pending?.complete(connected)
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) { service = null }
        override fun onBindingDied(name: ComponentName?) { service = null }
        override fun onNullBinding(name: ComponentName?) { pending?.completeExceptionally(IllegalStateException("Root service unavailable")) }
    }

    suspend fun inspect(context: Context, targets: List<NotificationAppTarget>): List<NotificationAppState> =
        operationLock.withLock {
            withContext(Dispatchers.IO) {
                val controller = connect(context.applicationContext)
                targets.chunked(100).flatMap { chunk ->
                    Json.decodeFromString<List<NotificationAppState>>(controller.inspect(user(context), Json.encodeToString(chunk)))
                }
            }
        }

    suspend fun apply(context: Context, mutations: List<NotificationMutation>): NotificationBatchReport =
        operationLock.withLock {
            withContext(Dispatchers.IO) {
                Json.decodeFromString(connect(context.applicationContext).apply(user(context), Json.encodeToString(mutations)))
            }
        }

    private fun user(context: Context): Int = context.applicationInfo.uid / 100000

    private suspend fun connect(context: Context): INotificationBatchController = connectionLock.withLock {
        service?.takeIf { it.asBinder().isBinderAlive }?.let { return@withLock it }
        val deferred = CompletableDeferred<INotificationBatchController>()
        pending = deferred
        main.post {
            runCatching { RootService.bind(Intent(context, NotificationBatchRootService::class.java), connection) }
                .onFailure(deferred::completeExceptionally)
        }
        try { withTimeout(12_000) { deferred.await() } }
        finally { if (pending === deferred) pending = null }
    }
}
