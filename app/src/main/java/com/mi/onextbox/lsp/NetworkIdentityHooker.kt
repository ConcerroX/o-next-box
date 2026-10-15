package com.mi.onextbox.lsp

import android.app.Application
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.SubscriptionInfo
import android.util.Log
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import com.mi.onextbox.lsp.compat.ModernHookBridge as Bridge
import com.mi.onextbox.lsp.compat.ModernMethodHook as Hook
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernReflect
import com.mi.onextbox.lsp.compat.MODERN_PREFERENCES_READY_KEY

/** System-wide property publication, with genuine values retained inside the phone process. */
internal object NetworkIdentityHooker {
    private const val TAG = "ONextBox-NetworkIdentity"
    private val lock = Any()
    private val bypass = ThreadLocal.withInitial { false }
    private val main by lazy { Handler(Looper.getMainLooper()) }
    private var installed = false
    @Volatile private var engine: NetworkIdentityProperties? = null
    @Volatile private var publishedProfiles: Map<Int, NetworkIdentityRules.Profile> = emptyMap()
    private var application: Context? = null
    private var preferences: SharedPreferences? = null
    private var registered = false
    private var propertyClass: Class<*>? = null
    private val update = Runnable { HotReloadLifecycle.withHook({}, { refreshSafely() }) }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in NetworkIdentitySettings.keys || key == MODERN_PREFERENCES_READY_KEY) schedule()
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { schedule() }
    }

    fun hook(loader: ClassLoader, processName: String) {
        if (processName != "com.android.phone" || installed) return
        installed = true
        val properties = ModernReflect.findClassIfExists("android.os.SystemProperties", loader) ?: return
        propertyClass = properties
        val setter = properties.getDeclaredMethod("set", String::class.java, String::class.java)
        Bridge.hookMethodFast(setter) { chain ->
            val key = chain.args[0] as? String
            if (bypass.get() == true || key !in NetworkIdentityRules.properties) chain.proceed()
            else synchronized(lock) {
                // Native telephony updates still run; only published identity is overridden afterwards.
                val result = chain.proceed()
                runCatching { engine?.afterNativeWrite(key!!, (chain.args[1] as? String).orEmpty()) }
                    .onFailure { log("Cannot journal native update", it) }
                refreshSafely()
                result
            }
        }
        properties.declaredMethods.filter { it.name == "get" && it.returnType == String::class.java }.forEach { getter ->
            Bridge.hookMethodFast(getter) { chain ->
                val key = chain.args.firstOrNull() as? String
                if (bypass.get() == true || key !in NetworkIdentityRules.properties) chain.proceed()
                else synchronized(lock) {
                    // Read and interpret under the same lock as publication. Otherwise a getter
                    // could read generation N and compare it with generation N+1's journal.
                    val value = chain.proceed() as String
                    engine?.actual(key!!, value) ?: value
                }
            }
        }
        // Generated sysprop accessors and TelephonyManager wrappers can otherwise inline get/set.
        listOf("android.sysprop.TelephonyProperties", "android.internal.telephony.sysprop.TelephonyProperties",
            "android.telephony.TelephonyManager").forEach { name ->
            ModernReflect.findClassIfExists(name, loader)?.declaredMethods?.filter {
                it.name.contains("operator", true) || it.name.contains("CountryIso", true)
            }?.forEach { runCatching { ModernHookRuntime.requireModule().deoptimize(it) } }
        }
        installCountryService(loader)
        installSubscriptionService(loader)
        Bridge.hookAllMethods(Application::class.java, "attach", object : Hook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.throwable == null) (param.args.firstOrNull() as? Context)?.let(::startAfterCommit)
            }
        })
        val current = runCatching {
            ModernReflect.callStaticMethod(ModernReflect.findClass("android.app.ActivityThread", loader), "currentApplication") as? Context
        }.getOrNull()
        if (current != null) startAfterCommit(current)
        HotReloadLifecycle.register("network-identity", detach = {
            unregister()
            synchronized(lock) { raw { engine?.refresh(null) }; publishedProfiles = emptyMap() }
        }, restore = { register(); refreshSafely() }, resumeAfterRejection = { register(); schedule() })
    }

    // Runs in the privileged phone process on the platform's already permission-filtered return
    // value, not in the module UI process. Original SecurityExceptions are never replaced.
    @SuppressLint("MissingPermission")
    private fun installCountryService(loader: ClassLoader) {
        val owner = ModernReflect.findClassIfExists("com.android.phone.PhoneInterfaceManager", loader) ?: return
        Bridge.hookAllMethods(owner, "getNetworkCountryIsoForPhone", object : Hook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // Keep framework/phone decisions genuine and preserve all original access checks.
                if (param.throwable != null || Binder.getCallingUid() % 100000 < Process.FIRST_APPLICATION_UID) return
                val slot = param.args.firstOrNull() as? Int ?: return
                synchronized(lock) {
                    if (engine == null) return
                    val profile = publishedProfiles[slot] ?: return
                    val numeric = raw { readProperty("gsm.operator.numeric") }
                    if (NetworkIdentityRules.decode(numeric).getOrNull(slot).isNullOrEmpty()) return
                    if (param.result is String) param.result = profile.region
                }
            }
        })
        owner.declaredMethods.filter { it.name in setOf("getServiceStateForSlot", "getServiceStateForSubscriber") &&
            it.returnType == ServiceState::class.java }.forEach { method ->
            Bridge.hookMethod(method, object : Hook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!externalSuccessful(param)) return
                    val original = param.result as? ServiceState ?: return
                    try {
                        if (original.operatorNumeric.isNullOrEmpty()) return
                        val argument = param.args.firstOrNull() as? Int ?: return
                        val slot = if (method.name == "getServiceStateForSlot") argument else runCatching {
                            ModernReflect.callStaticMethod(ModernReflect.findClass("android.telephony.SubscriptionManager", loader),
                                "getSlotIndex", argument) as Int
                        }.getOrDefault(-1)
                        val selected = publishedProfiles[slot] ?: return
                        val copy = ServiceState(original)
                        copy.setOperatorName(selected.name, selected.name, selected.numeric)
                        param.result = copy
                    } catch (_: SecurityException) {
                        // A platform rejecting access keeps its already-filtered original result.
                    }
                }
            })
        }
    }

    private fun externalSuccessful(param: Hook.MethodHookParam) = param.throwable == null && engine != null &&
        Binder.getCallingUid() % 100000 >= Process.FIRST_APPLICATION_UID

    private fun installSubscriptionService(loader: ClassLoader) {
        val owner = ModernReflect.findClassIfExists(
            "com.android.internal.telephony.subscription.SubscriptionManagerService", loader) ?: return
        // Clone the already permission-filtered result. Never mutate the subscription database or
        // remove a check on the Binder entrypoint; internal radio clients keep the genuine record.
        owner.declaredMethods.filter { it.name in setOf("getActiveSubscriptionInfo", "getActiveSubscriptionInfoForSimSlotIndex",
            "getActiveSubscriptionInfoList", "getAllSubInfoList", "getAvailableSubscriptionInfoList") }.forEach { method ->
            Bridge.hookMethod(method, object : Hook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!externalSuccessful(param)) return
                    val selectedProfiles = publishedProfiles
                    fun copy(value: SubscriptionInfo): SubscriptionInfo {
                        if (value.simSlotIndex < 0 || value.mccString.isNullOrEmpty()) return value
                        val selected = selectedProfiles[value.simSlotIndex] ?: return value
                        return runCatching {
                            val builderClass = ModernReflect.findClass("android.telephony.SubscriptionInfo\$Builder", loader)
                            val builder = builderClass.getDeclaredConstructor(SubscriptionInfo::class.java).newInstance(value)
                            ModernReflect.callMethod(builder, "setMcc", selected.numeric.take(3))
                            ModernReflect.callMethod(builder, "setMnc", selected.numeric.drop(3))
                            ModernReflect.callMethod(builder, "setCountryIso", selected.region)
                            ModernReflect.callMethod(builder, "setCarrierName", selected.name)
                            ModernReflect.callMethod(builder, "build") as SubscriptionInfo
                        }.getOrElse { value }
                    }
                    param.result = when (val result = param.result) {
                        is SubscriptionInfo -> copy(result)
                        is List<*> -> result.map { if (it is SubscriptionInfo) copy(it) else it }
                        else -> result
                    }
                }
            })
        }
    }

    private fun startAfterCommit(context: Context) = HotReloadLifecycle.afterCommit {
        if (application != null) return@afterCommit
        runCatching {
            synchronized(lock) {
                val protected = context.createDeviceProtectedStorageContext()
                val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
                check(boot >= 0) { "Boot identity unavailable; refusing unsafe property backup" }
                val journal = protected.getSharedPreferences("onextbox_network_identity_journal_v1", Context.MODE_PRIVATE)
                val saved = if (journal.getInt("boot", -1) != boot) emptyMap() else
                    NetworkIdentityRules.properties.mapNotNull { key ->
                        val original = journal.getString("$key.original", null) ?: return@mapNotNull null
                        val applied = journal.getString("$key.applied", null) ?: return@mapNotNull null
                        key to NetworkIdentityProperties.Record(original, applied, journal.getString("$key.previous", null))
                    }.toMap()
                engine = NetworkIdentityProperties(::readProperty, ::writeProperty, { records ->
                    val editor = journal.edit().clear().putInt("boot", boot)
                    records.forEach { (key, value) ->
                        editor.putString("$key.original", value.original).putString("$key.applied", value.applied)
                            .putString("$key.previous", value.previous)
                    }
                    check(editor.commit()) { "Cannot persist recovery journal" }
                }, saved)
                application = protected
                preferences = ModernHookRuntime.remotePreferences("lsp_features")
            }
            register()
            refreshSafely()
        }.onFailure { log("Initialization failed; no new overrides will be applied", it) }
    }

    private fun profiles(): Map<Int, NetworkIdentityRules.Profile> {
        val prefs = preferences ?: return emptyMap()
        if (!prefs.getBoolean(MODERN_PREFERENCES_READY_KEY, false)) return emptyMap()
        return NetworkIdentitySettings.read(prefs).profiles()
    }
    private fun register() {
        val context = application ?: return
        if (registered) return
        preferences?.registerOnSharedPreferenceChangeListener(listener)
        context.registerReceiver(receiver, IntentFilter().apply {
            addAction("android.intent.action.SIM_STATE_CHANGED")
            addAction("android.intent.action.SERVICE_STATE")
        }, Context.RECEIVER_EXPORTED)
        registered = true
    }
    private fun unregister() {
        main.removeCallbacks(update)
        if (!registered) return
        preferences?.unregisterOnSharedPreferenceChangeListener(listener)
        application?.unregisterReceiver(receiver)
        registered = false
    }
    private fun schedule() { main.removeCallbacks(update); main.postDelayed(update, 100) }
    private fun refreshSafely() {
        synchronized(lock) {
            if (bypass.get() == true || engine == null) return
            runCatching {
                val selected = profiles()
                raw { engine?.refreshSlots(selected) }
                publishedProfiles = selected
            }.onFailure {
                publishedProfiles = emptyMap()
                log("Property refresh failed; attempting restoration", it)
                runCatching { raw { engine?.refresh(null) } }
                    .onFailure { failure -> log("Restoration failed; recovery journal retained", failure) }
            }
        }
    }
    private fun readProperty(key: String) = ModernReflect.callStaticMethod(propertyClass!!, "get", key, "") as String
    private fun writeProperty(key: String, value: String) { ModernReflect.callStaticMethod(propertyClass!!, "set", key, value) }
    private inline fun <T> raw(block: () -> T): T {
        val previous = bypass.get(); bypass.set(true)
        return try { block() } finally { bypass.set(previous) }
    }
    private fun log(message: String, error: Throwable) = ModernHookRuntime.log(Log.WARN, TAG, message, error)
}
