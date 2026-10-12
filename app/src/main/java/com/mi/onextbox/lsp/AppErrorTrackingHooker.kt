package com.mi.onextbox.lsp

import android.app.ApplicationErrorReport
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import com.mi.onextbox.errors.AppErrorKind
import com.mi.onextbox.errors.AppErrorMetadata
import com.mi.onextbox.errors.AppErrorProvider
import com.mi.onextbox.errors.AppErrorRecord
import com.mi.onextbox.errors.AppErrorRules
import com.mi.onextbox.errors.AppErrorTrackingSettings
import com.mi.onextbox.errors.toBundle
import com.mi.onextbox.lsp.compat.HookConfigSnapshot
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernReflect
import io.github.libxposed.api.XposedInterface
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal object AppErrorTrackingHooker {
    private const val TAG = "ONextBox-AppErrors"
    private val pending = AtomicInteger()
    private val workerLock = Any()
    private var worker: ThreadPoolExecutor? = null

    fun hookSystemServer(loader: ClassLoader) {
        HotReloadLifecycle.register("errors:writer", canReload = { pending.get() == 0 }, detach = {
            synchronized(workerLock) {
                check(pending.get() == 0) { "Error report write is in progress" }
                worker?.let {
                    it.shutdown()
                    check(it.awaitTermination(250, TimeUnit.MILLISECONDS)) { "Error writer is busy" }
                }
                worker = null
            }
        }, restore = { synchronized(workerLock) { worker = null } })
        runCatching {
            val ams = ModernReflect.findClass("com.android.server.am.ActivityManagerService", loader)
            val candidates = ams.declaredMethods.filter {
                it.name == "addErrorToDropBox" && it.returnType == Void.TYPE &&
                    AppErrorRules.supportsSignature(it.parameterTypes.map(Class<*>::getName))
            }
            val size = candidates.maxOfOrNull { it.parameterCount } ?: error("Unsupported error reporting contract")
            val target = candidates.single { it.parameterCount == size }
            ModernHookRegistry.installFast("$TAG:report", target, XposedInterface.Hooker { chain ->
                if (HookConfigSnapshot.boolean(AppErrorTrackingSettings.KEY, false) == true) {
                    runCatching { capture(chain) }.onFailure { HookLog.w(TAG, "Unable to capture error metadata", it) }
                }
                chain.proceed()
            })
            HookLog.i(TAG, "Error observation ready: ${target.parameterCount} arguments")
        }.onFailure { HookLog.w(TAG, "Error observation unavailable; system behavior retained", it) }
    }

    private fun capture(chain: XposedInterface.Chain) {
        val crash = chain.getArg(9) as? ApplicationErrorReport.CrashInfo
        val kind = AppErrorRules.kind(chain.getArg(0) as? String, crash?.exceptionClassName) ?: return
        val process = chain.getArg(1)
        val info = process?.let { ModernReflect.getObjectField(it, "info") as? ApplicationInfo }
        val packageName = info?.packageName ?: if (process == null) "android" else return
        val processName = (chain.getArg(2) as? String)?.take(255) ?: packageName
        val pid = if (process == null) Process.myPid() else (ModernReflect.getObjectField(process, "mPid") as? Number)?.toInt() ?: 0
        val context = ModernReflect.getObjectField(requireNotNull(chain.thisObject), "mContext") as? Context ?: return
        val summaryParts = listOfNotNull(
            crash?.exceptionClassName, crash?.exceptionMessage,
            chain.getArg(6) as? String,
        )
        val summary = summaryParts.joinToString(": ") { it.take(AppErrorRules.MAX_SUMMARY) }.ifEmpty { kind.name }
        val (combined, truncated) = AppErrorRules.combineDetails(listOf(
            crash?.stackTrace,
            (chain.getArg(6) as? String)?.let { "Reason: $it" },
            chain.getArg(7) as? String,
        ))
        val metadata = AppErrorMetadata(
            activity = (chain.getArg(3) as? String).orEmpty(),
            parentActivity = (chain.getArg(4) as? String).orEmpty(),
            parentProcess = chain.getArg(5)?.let { runCatching { ModernReflect.getObjectField(it, "processName") as? String }.getOrNull() }.orEmpty(),
            exceptionClass = crash?.exceptionClassName.orEmpty(), exceptionMessage = crash?.exceptionMessage.orEmpty(),
            throwFile = crash?.throwFileName.orEmpty(), throwClass = crash?.throwClassName.orEmpty(),
            throwMethod = crash?.throwMethodName.orEmpty(), throwLine = crash?.throwLineNumber ?: -1,
            targetSdk = info?.targetSdkVersion ?: 0, minSdk = info?.minSdkVersion ?: 0,
            systemApp = info?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 },
            device = "${Build.MANUFACTURER} ${Build.MODEL}", androidVersion = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT, fingerprint = Build.FINGERPRINT, abi = Build.SUPPORTED_ABIS.joinToString(", "),
        )
        val record = AppErrorRecord(
            time = System.currentTimeMillis(), packageName = packageName, processName = processName,
            uid = info?.uid ?: 1000, pid = pid, kind = kind, summary = summary,
            detail = combined, truncated = truncated || summaryParts.any { it.length > AppErrorRules.MAX_SUMMARY }, metadata = metadata,
        )
        val bounded = AppErrorRules.bounded(record, 0) ?: return
        val trace = if (kind != AppErrorKind.CRASH) chain.getArg(8) as? File else null
        enqueue(context, bounded, trace)
    }

    private fun enqueue(context: Context, record: AppErrorRecord, trace: File?) {
        if (HotReloadLifecycle.isRetiring) return
        if (pending.incrementAndGet() > AppErrorRules.MAX_PENDING) {
            pending.decrementAndGet()
            return
        }
        try {
            synchronized(workerLock) {
                val executor = worker ?: ThreadPoolExecutor(
                    1, 1, 10, TimeUnit.SECONDS, ArrayBlockingQueue(AppErrorRules.MAX_PENDING),
                    { runnable -> Thread(runnable, "ONextBox-ErrorWriter").apply { isDaemon = true; priority = Thread.MIN_PRIORITY } },
                    ThreadPoolExecutor.AbortPolicy(),
                ).apply { allowCoreThreadTimeOut(true) }.also { worker = it }
                executor.execute {
                    try {
                        val complete = enrich(context, appendTrace(record, trace))
                        context.contentResolver.call(AppErrorProvider.URI, "append", null, complete.toBundle())
                    } catch (error: Exception) {
                        HookLog.w(TAG, "Unable to store error report", error)
                    } finally {
                        pending.decrementAndGet()
                    }
                }
            }
        } catch (error: Exception) {
            pending.decrementAndGet()
            HookLog.w(TAG, "Error report queue unavailable", error)
        }
    }

    private fun appendTrace(record: AppErrorRecord, trace: File?): AppErrorRecord {
        if (record.kind == AppErrorKind.CRASH) return record
        fun status(value: String) = record.copy(metadata = record.metadata.copy(traceStatus = value))
        if (trace == null) return status("not_provided")
        if (record.detail.length >= AppErrorRules.MAX_DETAIL) return status("truncated").copy(truncated = true)
        return runCatching {
            val file = trace.canonicalFile
            val allowedPath = if (record.kind == AppErrorKind.ANR) file.path.startsWith("/data/anr/")
                else record.kind == AppErrorKind.NATIVE && file.path.startsWith("/data/tombstones/")
            require(allowedPath && file.isFile && !file.name.endsWith(".pb"))
            val remaining = AppErrorRules.MAX_DETAIL - record.detail.length - 2
            if (remaining <= 0) return@runCatching status("truncated").copy(truncated = true)
            val buffer = CharArray(remaining + 1)
            val count = file.bufferedReader().use { reader ->
                var read = 0
                while (read < buffer.size) {
                    val next = reader.read(buffer, read, buffer.size - read)
                    if (next < 0) break
                    read += next
                }
                read
            }
            record.copy(
                detail = record.detail + "\n\n" + String(buffer, 0, count.coerceAtMost(remaining)),
                truncated = record.truncated || count > remaining,
                metadata = record.metadata.copy(traceStatus = if (count > remaining) "truncated" else if (count == 0) "unavailable" else "captured"),
            )
        }.getOrElse { status("unavailable") }
    }

    private fun enrich(context: Context, record: AppErrorRecord): AppErrorRecord = runCatching {
        val info = context.packageManager.getPackageInfo(record.packageName, 0)
        record.copy(
            appName = info.applicationInfo?.loadLabel(context.packageManager)?.toString()?.take(255).orEmpty(),
            version = "${info.versionName.orEmpty().take(200)} (${info.longVersionCode})",
        )
    }.getOrElse { record }
}
