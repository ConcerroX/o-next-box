package com.mi.onextbox.lsp

import android.os.Bundle
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect
import io.github.libxposed.api.XposedInterface
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean

/** Audited against Games 10.42.5. Unknown signatures retain stock behavior. */
internal object GameAssistantHooker {
    private const val TAG = "ONextBox-GameAssistant"

    fun hook(loader: ClassLoader, apkPath: String) {
        fun adapter(name: String, action: () -> Unit) = runCatching(action)
            .onFailure { HookLog.w(TAG, "$name unavailable; stock behavior retained", it) }
        adapter("Sidebar") { sidebar(loader) }
        adapter("Root safety state") { rootSafety(loader) }
        adapter("Skins") { skins(loader) }
        adapter("Developer options") { GameAssistantDeveloperController.hook(loader) }
        adapter("Semantic adapters") {
            System.loadLibrary("dexkit")
            DexKitBridge.create(apkPath).use { dex ->
                adapter("Promotion dialog") {
                    val data = dex.findMethod { matcher {
                        declaredClass = "business.module.toolsrecommend.ToolsRecommendAssistantDialogHelper"
                        usingStrings("isDialogShow  return")
                        returnType = "void"
                    } }.single()
                    val method = data.getMethodInstance(loader)
                    val types = method.parameterTypes.map { it.name }
                    require(types == List(4) { "kotlin.jvm.functions.Function0" } + List(6) { "java.lang.String" } +
                        listOf("java.lang.Boolean", "kotlin.jvm.functions.Function1"))
                    fast(method) { chain ->
                        if (!on(GameAssistantSettings.PROMOTIONS)) chain.proceed() else {
                            // Match the native no-show path: report false, never confirm, and
                            // never invoke onDismiss for a dialog that was not attached.
                            chain.args[11]?.let { ModernReflect.callMethod(it, "invoke", false) }
                            null
                        }
                    }
                    deoptimize(data.callers.mapNotNull { runCatching { it.getMethodInstance(loader) }.getOrNull() })
                }
                adapter("Promotion cards") {
                    val method = dex.findMethod { matcher {
                        declaredClass = "business.module.toolsrecommend.ToolsRecommendCardHelper"
                        usingStrings("isSupportShowGameCenterCard: isShowGameCenterCard = ")
                        returnType = "boolean"
                        paramTypes()
                    } }.single().getMethodInstance(loader)
                    replace(method, GameAssistantSettings.PROMOTIONS) { false }
                }
                adapter("Welfare notifications") {
                    val method = dex.findMethod { matcher {
                        declaredClass = "business.notification.WelfareNotificationHelper"
                        usingStrings("checkAndSendWelfareNotification installGameList isNotEmpty")
                        paramTypes("kotlin.coroutines.Continuation")
                        returnType = "java.lang.Object"
                    } }.single().getMethodInstance(loader)
                    val unit = listOf("kotlin.q", "kotlin.Unit").firstNotNullOfOrNull { name ->
                        ModernReflect.findClassIfExists(name, loader)?.let { type ->
                            type.declaredFields.firstOrNull { Modifier.isStatic(it.modifiers) && it.type == type }
                                ?.apply { isAccessible = true }?.get(null)
                        }
                    } ?: error("Host Kotlin Unit unavailable")
                    replace(method, GameAssistantSettings.PROMOTIONS) { unit }
                }
                adapter("Local Root probes") {
                    val owner = dex.findMethod { matcher {
                        usingStrings("/system/app/Superuser.apk")
                        returnType = "boolean"
                        paramTypes()
                    } }.single().getMethodInstance(loader).declaringClass
                    val probes = owner.declaredMethods.filter {
                        Modifier.isStatic(it.modifiers) && it.returnType == Boolean::class.javaPrimitiveType &&
                            it.parameterCount == 0
                    }
                    require(probes.size in 3..5)
                    probes.forEach { replace(it, GameAssistantSettings.BLOCK_ROOT) { false } }
                    deoptimize(owner.declaredMethods.toList())
                    dex.findMethod { matcher { declaredClass = owner.name; returnType = "boolean"; paramTypes() } }
                        .forEach { probe -> deoptimize(probe.callers.mapNotNull { runCatching { it.getMethodInstance(loader) }.getOrNull() }) }
                }
                adapter("Skin and safety callers") {
                    listOf(
                        Triple("com.coloros.gamespaceui.helper.d", "g", "java.lang.String"),
                        Triple("com.oplus.cosa.COSASDKManager", "getSupportCoolEx", "android.os.Bundle"),
                    ).forEach { (owner, name, result) ->
                        val target = dex.findMethod { matcher { declaredClass = owner; this.name = name; returnType = result; paramTypes() } }.single()
                        deoptimize(target.callers.mapNotNull { runCatching { it.getMethodInstance(loader) }.getOrNull() })
                    }
                }
            }
        }
    }

    private fun on(key: String) = GameAssistantSettings.enabledXposed(key)

    private fun fast(method: Method, block: (XposedInterface.Chain) -> Any?) {
        ModernHookRegistry.installFast("$TAG:${method.toGenericString()}", method, XposedInterface.Hooker(block))
        deoptimize(listOf(method))
        HookLog.i(TAG, "Installed ${method.declaringClass.name}.${method.name}")
    }

    private fun replace(method: Method, key: String, value: () -> Any?) =
        fast(method) { chain -> if (on(key)) value() else chain.proceed() }

    private fun deoptimize(methods: List<Method>) {
        methods.distinct().forEach { runCatching { ModernHookRuntime.requireModule().deoptimize(it) }
            .onFailure { error -> HookLog.w(TAG, "Caller deoptimization unavailable: ${it.name}", error) } }
    }

    private fun sidebar(loader: ClassLoader) {
        val provider = ModernReflect.findClass("business.gamedock.sort.AppDataProviderCommon", loader)
        // Both helpers cache the max into final static fields during <clinit>. Update the
        // audited cache at the guard, including restoring its stock value when disabled.
        maxCache(loader, "business.module.customdefine.apps.DesignatedAppsHelper", "v", arrayOf(Boolean::class.javaPrimitiveType!!), 20)
        val bookmark = ModernReflect.findClassIfExists("business.mainpanel.bookmarks.adapter.DesignatedBookMarksHelper", loader)
        if (bookmark != null) {
            val bean = ModernReflect.findClass("business.mainpanel.bookmarks.data.CustomServiceBean", loader)
            maxCache(loader, bookmark.name, "i", arrayOf(bean), 8)
        }
        replace(provider.getDeclaredMethod("j"), GameAssistantSettings.UNLIMITED) { Int.MAX_VALUE }
        ModernReflect.findClassIfExists("p2.a", loader)?.let { type ->
            val getter = type.getDeclaredMethod("a")
            require(getter.returnType == Int::class.javaPrimitiveType)
            replace(getter, GameAssistantSettings.UNLIMITED) { Int.MAX_VALUE }
        }
    }

    private fun maxCache(loader: ClassLoader, ownerName: String, name: String, params: Array<Class<*>>, stock: Int) {
        val owner = ModernReflect.findClass(ownerName, loader)
        val fields = owner.declaredFields.filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
        val method = owner.getDeclaredMethod(name, *params)
        val lock = Any()
        var cache: Field? = null
        val failed = AtomicBoolean()
        ModernHookRegistry.installCompat("$TAG:limit:${owner.name}", method, object : ModernMethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // Do not initialize AppDataProvider before Environment._isExport is ready.
                // A real helper instance reaching this guard proves its <clinit> completed.
                runCatching {
                    val field = synchronized(lock) {
                        cache ?: fields.single { it.isAccessible = true; it.getInt(null) in setOf(stock, Int.MAX_VALUE) }
                            .also { resolved ->
                                cache = resolved
                                HotReloadLifecycle.register("$TAG:cache:${owner.name}",
                                    capture = { resolved.getInt(null) }, canReload = { true },
                                    detach = { if (resolved.getInt(null) != stock) resolved.setInt(null, stock) },
                                    restore = { value ->
                                        val saved = value as? Int ?: stock
                                        if (resolved.getInt(null) != saved) resolved.setInt(null, saved)
                                    })
                            }
                    }
                    val limit = GameAssistantRules.maxItems(on(GameAssistantSettings.UNLIMITED), stock)
                    if (field.getInt(null) != limit) field.setInt(null, limit)
                }.onFailure { if (failed.compareAndSet(false, true)) HookLog.w(TAG, "Sidebar cache unchanged: ${owner.name}", it) }
            }
        })
        deoptimize(owner.declaredMethods.toList())
        HookLog.i(TAG, "Lazy sidebar max adapter installed: ${owner.name}")
    }

    private fun rootSafety(loader: ClassLoader) {
        ModernReflect.findClassIfExists("com.platform.usercenter.tools.device.UCDeviceInfoUtil", loader)?.let { type ->
            replace(type.getDeclaredMethod("isRoot"), GameAssistantSettings.BLOCK_ROOT) { false }
        }
        val cosa = ModernReflect.findClass("com.oplus.cosa.COSASDKManager", loader)
        val method = cosa.getDeclaredMethod("getSupportCoolEx")
        require(method.returnType == Bundle::class.java)
        ModernHookRegistry.installCompat("$TAG:cool-ex", method, object : ModernMethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!on(GameAssistantSettings.BLOCK_ROOT)) return
                val original = param.result as? Bundle ?: return
                if (original.getInt("isSafe") != 1) return
                param.result = Bundle(original).apply { putInt("isSafe", 0) }
            }
        })
        deoptimize(listOf(method))
        listOf("business.module.gamefilter.GameFilterFeature", "business.module.gamefilter.global.GameGlobalFilterFeature").forEach { name ->
            val owner = ModernReflect.findClassIfExists(name, loader) ?: return@forEach
            owner.declaredMethods.filter {
                it.name == "safetyStatusChange" && it.parameterTypes.contentEquals(arrayOf(String::class.java, Int::class.javaPrimitiveType))
            }.forEach { callback ->
                ModernHookRegistry.installCompat("$TAG:safety:${callback.toGenericString()}", callback, object : ModernMethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[1] = GameAssistantRules.safetyStatus(on(GameAssistantSettings.BLOCK_ROOT), param.args[1] as Int)
                    }
                })
            }
            deoptimize(owner.declaredMethods.toList())
        }
    }

    private fun skins(loader: ClassLoader) {
        // This getter is the native skin loader's source of truth, not the whole-device
        // model property. Default means ordinary UI, not a forged limited-edition phone.
        val helper = ModernReflect.findClass("com.coloros.gamespaceui.helper.d", loader)
        val getter = helper.getDeclaredMethod("g")
        require(Modifier.isStatic(getter.modifiers) && getter.returnType == String::class.java)
        replace(getter, GameAssistantSettings.SKINS) { GameAssistantRules.skinName(GameAssistantSettings.skinXposed()) }
        deoptimize(helper.declaredMethods.filter { it.name in setOf("g", "r") })
        ModernReflect.findClassIfExists("business.GameSpaceApplication", loader)?.let { owner ->
            deoptimize(owner.declaredMethods.filter { it.name == "G" })
        }
        ModernReflect.findClassIfExists("business.util.j0", loader)?.let { deoptimize(it.declaredMethods.toList()) }
    }
}
