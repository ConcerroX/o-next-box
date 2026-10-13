package com.mi.onextbox.lsp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernMethodHook
import java.lang.reflect.Method
import java.util.WeakHashMap

/** Filters only status-bar icon holders; telephony and carrier models stay native. */
internal object DataSimSignalHooker {
    private const val TAG = "ONextBox-DataSimSignal"
    private const val OWNER = "$TAG:owner"
    private const val DATA_CHANGED = "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"

    fun hook(loader: ClassLoader) {
        runCatching {
            val controller = Class.forName("com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl", false, loader)
            val update = controller.getDeclaredMethod("setNewMobileIconSubIds", List::class.java)
            val contextField = controller.getDeclaredField("mContext").apply { isAccessible = true }
            val stack = Class.forName("com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MobileIconsViewModel\$isStackable\$1", false, loader)
            val calculateStack = stack.getDeclaredMethod("invokeSuspend", Any::class.java)
            val owner = Owner(update)
            ModernHookRegistry.installCompat("$TAG:icons", update, object : ModernMethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val target = param.thisObject ?: return
                    val ids = (param.args[0] as? List<*>)?.map { (it as Number).toInt() } ?: return
                    val selected = DataSimSignalRules.select(ids, DataSimSignalSettings.enabledXposed(), dataSubId())
                    owner.remember(target, ids, selected)
                    // Native code reverses this list in place. Never pass the cached
                    // source or a singleton/immutable filtered list to it.
                    param.args[0] = ArrayList(selected)
                    val context = contextField.get(target) as Context
                    HotReloadLifecycle.afterCommit { owner.attach(context.applicationContext) }
                }
            })
            ModernHookRegistry.installCompat("$TAG:stack", calculateStack, object : ModernMethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.throwable == null && param.result is Boolean &&
                        DataSimSignalSettings.enabledXposed() && dataSubId() >= 0) param.result = false
                }
            })
            HotReloadLifecycle.register(OWNER, capture = owner::capture, detach = owner::detach,
                restore = { state -> owner.restore(state) })
            HotReloadLifecycle.restoredState(OWNER)?.let { state ->
                HotReloadLifecycle.afterCommit { owner.restore(state) }
            }
            stack.declaredMethods.filter { it.name == "invoke" }.forEach {
                runCatching { ModernHookRuntime.requireModule().deoptimize(it) }
                    .onFailure { error -> HookLog.w(TAG, "Could not deoptimize stacked-layout caller", error) }
            }
            HookLog.i(TAG, "Data-SIM status-bar filtering hooks installed")
        }.onFailure { HookLog.w(TAG, "Data-SIM signal adapter unavailable", it) }
    }

    private fun dataSubId() = runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }.getOrDefault(-1)

    private class Owner(private val update: Method) {
        private data class Entry(val source: List<Int>, val rendered: List<Int>)
        private val controllers = WeakHashMap<Any, Entry>()
        private val main = Handler(Looper.getMainLooper())
        private var context: Context? = null
        private val refresh = Runnable {
            HotReloadLifecycle.withHook({}, {
                val snapshots = synchronized(controllers) { controllers.entries.map { it.key to it.value } }
                snapshots.forEach { (target, entry) ->
                    val selected = DataSimSignalRules.select(entry.source, DataSimSignalSettings.enabledXposed(), dataSubId())
                    if (selected != entry.rendered) runCatching {
                        update.invoke(target, ArrayList(entry.source))
                    }.onFailure { HookLog.w(TAG, "Could not refresh data-SIM icon", it) }
                }
            })
        }
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == DATA_CHANGED) {
                    main.removeCallbacks(refresh)
                    main.post(refresh)
                }
            }
        }

        fun remember(target: Any, source: List<Int>, rendered: List<Int>) {
            val changed = synchronized(controllers) {
                val previous = controllers.put(target, Entry(source.toList(), rendered.toList()))
                previous?.rendered != rendered
            }
            if (changed && source != rendered) HookLog.d(TAG, "Showing data SIM ${rendered.firstOrNull()} of ${source.size} subscriptions")
        }

        fun attach(ctx: Context) {
            if (context != null) return
            runCatching {
                ctx.registerReceiver(receiver, IntentFilter(DATA_CHANGED), Context.RECEIVER_EXPORTED)
                context = ctx
            }.onFailure { HookLog.w(TAG, "Data-SIM change listener unavailable", it) }
        }

        fun capture(): Map<String, Any?> = mapOf("context" to context,
            "controllers" to synchronized(controllers) { controllers.entries.map { listOf(it.key, it.value.source) } })

        fun detach() {
            main.removeCallbacks(refresh)
            context?.unregisterReceiver(receiver)
            context = null
            synchronized(controllers) { controllers.clear() }
        }

        fun restore(state: Any?) {
            val saved = state as? Map<*, *> ?: return
            (saved["controllers"] as? List<*>)?.forEach { item ->
                val pair = item as? List<*> ?: return@forEach
                val target = pair.getOrNull(0) ?: return@forEach
                val ids = (pair.getOrNull(1) as? List<*>)?.map { (it as Number).toInt() } ?: return@forEach
                synchronized(controllers) { controllers[target] = Entry(ids, emptyList()) }
            }
            (saved["context"] as? Context)?.let(::attach)
            main.post(refresh)
        }
    }
}
