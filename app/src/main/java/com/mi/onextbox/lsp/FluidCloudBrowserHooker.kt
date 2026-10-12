package com.mi.onextbox.lsp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Modifier

internal object FluidCloudBrowserHooker {
    private const val TAG = "FluidCloudBrowser"
    private const val TEXT_INTENT = "com.oplus.textintent.distribution.bean.TextIntent"

    fun hook(loader: ClassLoader) {
        val key = "$TAG:invoke@${System.identityHashCode(loader)}"
        runCatching {
            val type = loader.loadClass(TEXT_INTENT)
            val invoke = type.getDeclaredMethod("processInvoke", Context::class.java, Intent::class.java)
            check(invoke.returnType == Void.TYPE && !Modifier.isStatic(invoke.modifiers))
            val packageName = type.getDeclaredField("packageName").apply {
                check(this.type == String::class.java && !Modifier.isStatic(modifiers))
                isAccessible = true
            }
            val textType = type.getDeclaredField("textType").apply {
                check(this.type == Int::class.javaObjectType && !Modifier.isStatic(modifiers))
                isAccessible = true
            }
            val intentType = type.getDeclaredField("intentType").apply {
                check(this.type == Int::class.javaPrimitiveType && !Modifier.isStatic(modifiers))
                isAccessible = true
            }
            ModernHookRegistry.installFast(key, invoke, XposedInterface.Hooker { chain ->
                if (!FluidCloudBrowserSettings.isEnabledXposed()) return@Hooker chain.proceed()
                val owner = chain.thisObject ?: return@Hooker chain.proceed()
                val context = chain.args.getOrNull(0) as? Context ?: return@Hooker chain.proceed()
                val original = chain.args.getOrNull(1) as? Intent ?: return@Hooker chain.proceed()
                val prepared = runCatching {
                    val oldPackage = packageName.get(owner) as? String
                    if (!FluidCloudBrowserRules.accepts(
                            enabled = true,
                            textType = textType.get(owner) as? Int,
                            intentType = intentType.getInt(owner),
                            packageName = oldPackage,
                            action = original.action,
                            scheme = original.data?.scheme,
                            host = original.data?.host,
                            explicitPackage = original.`package`,
                            componentPackage = original.component?.packageName,
                            hasSelector = original.selector != null,
                        )) return@runCatching null
                    val target = defaultBrowser(context) ?: return@runCatching null
                    val redirected = Intent(original).apply {
                        component = null
                        setPackage(target)
                        addCategory(Intent.CATEGORY_BROWSABLE)
                    }
                    val activity = redirected.resolveActivityInfo(context.packageManager, PackageManager.MATCH_DEFAULT_ONLY)
                        ?: return@runCatching null
                    if (activity.packageName != target || !activity.enabled || !activity.exported ||
                        !activity.applicationInfo.enabled) return@runCatching null
                    Triple(oldPackage, target, redirected)
                }.onFailure { HookLog.w(TAG, "Browser routing unavailable, stock behavior retained") }.getOrNull()
                    ?: return@Hooker chain.proceed()
                val args = chain.args.toTypedArray()
                args[1] = prepared.third
                FluidCloudBrowserRules.withTarget(
                    prepared.first, prepared.second, { packageName.set(owner, it) },
                ) { chain.proceed(args) }
            })
            type.declaredMethods.filter {
                it.name == "invoke" || it.name == "access\$processInvoke"
            }.forEach { ModernHookRuntime.requireModule().deoptimize(it) }
            runCatching { loader.loadClass("$TEXT_INTENT\$invoke\$1\$1\$1") }.getOrNull()
                ?.declaredMethods?.filter { it.name == "invokeSuspend" }
                ?.forEach { ModernHookRuntime.requireModule().deoptimize(it) }
            HookLog.i(TAG, "Recognized web link routing installed")
        }.onFailure {
            ModernHookRegistry.unhook(key)
            HookLog.w(TAG, "Web link target unavailable, stock behavior retained", it)
        }
    }

    private fun defaultBrowser(context: Context): String? {
        val pm = context.packageManager
        val lookup = runCatching {
            val user = Context::class.java.getMethod("getUserId").invoke(context) as Int
            pm.javaClass.getMethod("getDefaultBrowserPackageNameAsUser", Int::class.javaPrimitiveType)
                .invoke(pm, user) as? String
        }
        if (lookup.isSuccess) return lookup.getOrNull()?.takeIf { it.isNotBlank() }
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://onextbox.invalid/"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val resolved = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        val handlesWeb = runCatching {
            resolved.javaClass.getField("handleAllWebDataURI").getBoolean(resolved)
        }.getOrDefault(false)
        return resolved.activityInfo?.packageName?.takeIf {
            handlesWeb && it != "android" && resolved.activityInfo.enabled &&
                resolved.activityInfo.exported && resolved.activityInfo.applicationInfo.enabled
        }
    }
}
