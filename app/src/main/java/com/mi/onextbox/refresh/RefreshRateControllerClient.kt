package com.mi.onextbox.refresh

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.mi.onextbox.ui.common.AppLogStore
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class RefreshRateWriteTimeout : Exception("Refresh rate write did not finish before its deadline")

object RefreshRateControllerClient {
    private const val CONNECTION_TIMEOUT_MS = 4_000L
    private val lock = Any()
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runner = RefreshRateTaskRunner()
    private val launcher = RefreshRateTaskRunner(1)
    private var connection: Connection? = null
    private var retryAfter = 0L

    private class Connection : ServiceConnection {
        val pending = CompletableDeferred<IRefreshRateController?>()
        var controller: IRefreshRateController? = null
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val value = IRefreshRateController.Stub.asInterface(service)
            synchronized(lock) {
                if (connection !== this) { unbind(this); return }
                controller = value
                pending.complete(value)
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) = invalidate(this)
        override fun onBindingDied(name: ComponentName?) = invalidate(this)
        override fun onNullBinding(name: ComponentName?) = invalidate(this)
    }

    suspend fun getSupportedModes(context: Context): List<String>? = call(context, 7_000) {
        it.supportedModes?.toList().orEmpty()
    }
    suspend fun isRefreshRateDisplayEnabled(context: Context): Boolean? = call(context) { it.isRefreshRateDisplayEnabled }
    suspend fun setRefreshRateDisplayEnabled(context: Context, enabled: Boolean): Boolean? =
        call(context, write = true) { it.setRefreshRateDisplayEnabled(enabled) }
    suspend fun setRefreshRateMode(context: Context, modeIndex: Int): Boolean? = call(context, write = true) { it.setRefreshRateMode(modeIndex) }
    suspend fun resetRefreshRateMode(context: Context): Boolean? = call(context, write = true) { it.resetRefreshRateMode() }

    private suspend fun <T> call(context: Context, timeout: Long = 3_000, write: Boolean = false,
        block: (IRefreshRateController) -> T): T? {
        val active = getConnection(context.applicationContext ?: context) ?: return null
        val service = synchronized(lock) { active.controller } ?: return null
        return try {
            runner.run(timeout) { block(service) }.also {
                if (it == null) {
                    AppLogStore.w("RefreshRate", "Root service operation timed out")
                    invalidate(active)
                    // A Binder transaction may still be executing remotely. Do not
                    // launch a second write via the shell after an uncertain result.
                    if (write) throw RefreshRateWriteTimeout()
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (timeout: RefreshRateWriteTimeout) {
            throw timeout
        } catch (error: Exception) {
            AppLogStore.w("RefreshRate", "Root service operation failed: ${error.javaClass.simpleName}")
            invalidate(active)
            null
        }
    }

    private suspend fun getConnection(context: Context): Connection? {
        val active: Connection
        var bind = false
        synchronized(lock) {
            connection?.takeIf { it.controller?.asBinder()?.isBinderAlive == true }?.let { return it }
            if (SystemClock.elapsedRealtime() < retryAfter) return null
            active = connection ?: Connection().also { connection = it; bind = true }
        }
        if (bind) main.post {
            if (synchronized(lock) { connection !== active }) return@post
            runCatching {
                val task = RootService.bindOrTask(Intent(context, SurfaceFlingerRootService::class.java),
                    { work -> main.post(work) }, active)
                if (task != null) scope.launch {
                    try {
                        val started = launcher.run(CONNECTION_TIMEOUT_MS) {
                            // Default bind queues behind the shared LSP/touch shell.
                            Shell.Builder.create().setTimeout(3).build().use { shell ->
                                if (!shell.isRoot) false else { shell.execTask(task); true }
                            }
                        }
                        if (started != true) invalidate(active)
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { invalidate(active) }
                }
            }.onFailure { invalidate(active) }
        }
        val service = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) { active.pending.await() }
        if (service == null) { invalidate(active); return null }
        return active.takeIf { synchronized(lock) { connection === active } }
    }

    private fun invalidate(active: Connection) {
        synchronized(lock) {
            if (connection !== active) return
            connection = null
            retryAfter = SystemClock.elapsedRealtime() + 2_000
            active.controller = null
            active.pending.complete(null)
        }
        unbind(active)
    }

    private fun unbind(active: Connection) {
        main.post { runCatching { RootService.unbind(active) } }
    }
}
