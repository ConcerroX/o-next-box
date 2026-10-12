package com.mi.onextbox.lsp

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import com.mi.onextbox.lsp.compat.MODERN_PREFERENCES_READY_KEY
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import io.github.libxposed.api.XposedInterface
import java.util.WeakHashMap

internal object FluidCloudMediaBlacklistHooker {
    private const val TAG = "FluidCloudMediaBlacklist"

    fun hook(loader: ClassLoader) {
        val stateKey = "$TAG:state@${System.identityHashCode(loader)}"
        val saved = HotReloadLifecycle.restoredState(stateKey) as? Array<*>
        val session = Session(loader)
        HotReloadLifecycle.register(stateKey, capture = session::capture, detach = session::detach,
            restore = session::restore, resumeAfterRejection = session::schedule)
        session.install(saved?.getOrNull(0) as? Array<*>)
        HotReloadLifecycle.afterCommit { if (saved == null) session.restore(null) }
    }

    private class Session(private val loader: ClassLoader) {
        private val prefix = "$TAG@${System.identityHashCode(loader)}:"
        private val main = Handler(Looper.getMainLooper())
        private val seedlings = WeakHashMap<Any, Boolean>()
        @Volatile private var targets: FluidCloudMediaTargets? = null
        private var preferences: SharedPreferences? = null
        private val refresh = Runnable {
            HotReloadLifecycle.withHook<Unit>({}) {
                if (targets == null) install(null)
                val native = targets ?: return@withHook
                native.existingSeedlings(loader).forEach { owner -> synchronized(seedlings) { seedlings[owner] = true } }
                val owners = synchronized(seedlings) { seedlings.keys.toList() }
                owners.forEach { owner ->
                    runCatching { native.refresh.invoke(owner, 0, "ONextBox media blacklist") }
                        .onFailure { HookLog.w(TAG, "Media island refresh unavailable", it) }
                }
            }
        }
        private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == FluidCloudMediaSettings.ENABLED || key == FluidCloudMediaSettings.PACKAGES ||
                key == MODERN_PREFERENCES_READY_KEY) schedule()
        }

        fun install(saved: Array<*>?) {
            if (targets != null || HotReloadLifecycle.isRetiring) return
            if (saved == null && !FluidCloudMediaSettings.isEnabledXposed()) return
            runCatching {
                val native = saved?.let(::FluidCloudMediaTargets) ?: FluidCloudMediaTargets.resolve(loader)
                ModernHookRegistry.installFast("${prefix}entries", native.entries, XposedInterface.Hooker { chain ->
                    val original = chain.proceed()
                    if (targets == null) return@Hooker original
                    val entries = original as? List<*> ?: return@Hooker original
                    FluidCloudMediaRules.filter(entries, FluidCloudMediaSettings.blockedXposed()) { row ->
                        runCatching { native.packageName.invoke(row) as? String }.getOrNull()
                    }
                })
                ModernHookRegistry.installFast("${prefix}update", native.update, XposedInterface.Hooker { chain ->
                    if (targets != null) chain.thisObject?.let { synchronized(seedlings) { seedlings[it] = true } }
                    chain.proceed()
                })
                native.constructors.forEachIndexed { index, constructor ->
                    ModernHookRegistry.installFast("${prefix}constructor:$index", constructor, XposedInterface.Hooker { chain ->
                        val result = chain.proceed()
                        if (targets != null) chain.thisObject?.let { synchronized(seedlings) { seedlings[it] = true } }
                        result
                    })
                }
                native.callers.forEach { ModernHookRuntime.requireModule().deoptimize(it) }
                targets = native
                HookLog.i(TAG, "Media island package filter installed")
            }.onFailure {
                ModernHookRegistry.unhookPrefix(prefix)
                HookLog.w(TAG, "Media island targets unavailable, stock behavior retained", it)
            }
        }

        fun schedule() {
            if (HotReloadLifecycle.isRetiring) return
            main.removeCallbacks(refresh)
            main.postDelayed(refresh, 80L)
        }

        fun capture(): Array<Any?> = arrayOf(targets?.values,
            synchronized(seedlings) { seedlings.keys.toTypedArray() })

        fun detach() {
            main.removeCallbacks(refresh)
            preferences?.unregisterOnSharedPreferenceChangeListener(listener)
            preferences = null
            synchronized(seedlings) { seedlings.clear() }
        }

        fun restore(saved: Any?) {
            val values = saved as? Array<*>
            val native = targets
            (values?.getOrNull(1) as? Array<*>)?.filterNotNull()?.forEach { owner ->
                if (native?.seedlingType?.isInstance(owner) == true) synchronized(seedlings) { seedlings[owner] = true }
            }
            if (preferences == null) {
                ModernHookRuntime.remotePreferences("lsp_features")?.let {
                    it.registerOnSharedPreferenceChangeListener(listener)
                    preferences = it
                }
            }
            schedule()
        }
    }
}
