package com.mi.onextbox.lsp

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.provider.Settings
import com.mi.onextbox.lsp.LspConfig.PermissionFeature
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import com.mi.onextbox.lsp.compat.ModernHookBridge as XposedBridge
import com.mi.onextbox.lsp.compat.ModernMethodHook as XC_MethodHook
import com.mi.onextbox.lsp.compat.ModernReflect as XposedHelpers
import java.util.concurrent.atomic.AtomicBoolean

internal object PermissionSystemHooker {
    private const val TAG = "ONextBox-PermissionSystem"
    private val installed = AtomicBoolean(false)
    private val observing = AtomicBoolean(false)
    private var clearObserver: ContentObserver? = null
    private var observerContext: Context? = null
    private var observerController: Any? = null
    private var observerHandler: Handler? = null

    fun hook(loader: ClassLoader) {
        if (!installed.compareAndSet(false, true)) return
        HotReloadLifecycle.register("permission-system-observer",
            capture = { arrayOf(observerContext, observerController) },
            detach = {
                clearObserver?.let { observerContext?.contentResolver?.unregisterContentObserver(it) }
                observerHandler?.removeCallbacksAndMessages(null)
                clearObserver = null
                observerHandler = null
                observing.set(false)
            },
            restore = { value ->
                val state = value as? Array<*>
                val context = state?.getOrNull(0) as? Context
                val controller = state?.getOrNull(1)
                if (context != null && controller != null) {
                    observeClearRequests(context, controller)
                    observing.set(true)
                }
            },
        )
        runCatching {
            val target = XposedHelpers.findClassIfExists(
                "com.android.server.wm.OplusAppStartConfirmManager", loader,
            ) ?: return@runCatching
            val methods = target.declaredMethods.filter {
                it.name == "checkMaliciousIntercept" && it.returnType.name == "android.util.Pair"
            }
            methods.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (LspConfig.isPermissionFeatureEnabledXposed(PermissionFeature.DisableMaliciousAppIntercept)) {
                            // The caller treats null as no malicious interception, then continues
                            // its regular activity-start confirmation and Android permission checks.
                            param.result = null
                        }
                    }
                })
            }
            HookLog.i(TAG, "Malicious app-start intercept: ${methods.size} hooks installed")
        }.onFailure { HookLog.w(TAG, "Unable to hook malicious app-start interception", it) }

        runCatching {
            val target = XposedHelpers.findClassIfExists(
                "com.android.server.am.OplusActivityStartController", loader,
            ) ?: return@runCatching
            XposedBridge.hookAllMethods(target, "init", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (HotReloadLifecycle.isRetiring) return
                    if (param.throwable != null) return
                    val context = param.args.firstOrNull() as? Context ?: return
                    val controller = param.thisObject ?: return
                    if (!observing.compareAndSet(false, true)) return
                    runCatching { observeClearRequests(context, controller) }
                        .onFailure {
                            observing.set(false)
                            HookLog.w(TAG, "Unable to register clear-list action", it)
                        }
                }
            })
            HookLog.i(TAG, "Official activity-start whitelist reset hooked")
        }.onFailure { HookLog.w(TAG, "Unable to hook activity-start whitelist reset", it) }
    }

    private fun observeClearRequests(context: Context, controller: Any) {
        if (HotReloadLifecycle.isRetiring) return
        clearObserver?.let { observerContext?.contentResolver?.unregisterContentObserver(it) }
        observerHandler?.removeCallbacksAndMessages(null)
        clearObserver = null
        val resolver = context.contentResolver
        // Ignore persisted requests on restart: a one-shot UI action must never replay at boot.
        var lastRequest = Settings.Global.getString(resolver, PermissionStartAllowList.REQUEST_KEY)
        val controllerLooper = runCatching { (XposedHelpers.getObjectField(controller, "mHandler") as? Handler)?.looper }
            .getOrNull() ?: Looper.getMainLooper()
        // Own the queue endpoint, never remove unrelated messages from the OEM controller's handler.
        val handler = Handler(controllerLooper)
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                HotReloadLifecycle.withHook(fallback = {}, action = ::handleClearRequest)
            }

            private fun handleClearRequest() {
                if (HotReloadLifecycle.isRetiring) return
                val request = Settings.Global.getString(resolver, PermissionStartAllowList.REQUEST_KEY)
                    ?: return
                if (request == lastRequest) return
                lastRequest = request
                val parts = request.split('|')
                val id = parts.firstOrNull()?.takeIf { it.matches(Regex("[0-9a-f-]{36}")) } ?: return
                val result = runCatching {
                    check(LspConfig.isPermissionFeatureEnabledXposed(PermissionFeature.AlwaysAllowAppStart))
                    check(parts.size == 2)
                    val ids = parts[1].split(',').map { it.toInt() }.distinct()
                    check(ids.isNotEmpty() && ids.size <= 100)
                    val users = context.getSystemService(UserManager::class.java)
                    ids.forEach { userId ->
                        check(userId >= 0 && XposedHelpers.callMethod(users, "getUserInfo", userId) != null)
                    }
                    ids.forEach { XposedHelpers.callMethod(controller, "onUserRemoved", it) }
                    HookLog.i(TAG, "Cleared user-set app-start allow records through the official controller")
                }
                result.onFailure { HookLog.w(TAG, "Could not clear app-start allow records", it) }
                Settings.Global.putString(resolver, PermissionStartAllowList.RESULT_KEY,
                    "$id:${if (result.isSuccess) "ok" else "failed"}")
            }
        }
        clearObserver = observer
        observerContext = context
        observerController = controller
        observerHandler = handler
        resolver.registerContentObserver(Settings.Global.getUriFor(PermissionStartAllowList.REQUEST_KEY), false, observer)
    }
}
