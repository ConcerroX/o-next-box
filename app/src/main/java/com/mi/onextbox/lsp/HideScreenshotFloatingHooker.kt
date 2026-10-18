package com.mi.onextbox.lsp

import android.view.SurfaceControl
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect
import java.lang.reflect.Modifier

internal object HideScreenshotFloatingHooker {
    fun hook(loader: ClassLoader) {
        runCatching {
            val utils = ModernReflect.findClass("com.oplus.screenshot.common.util.WindowExcludeUtils", loader)
            val info = ModernReflect.findClass("com.oplus.app.OplusWindowInfo", loader)
            val surface = info.getField("mSurfaceControl")
            val mode = info.getField("windowingMode")
            val windowType = info.getField("type")
            val pkg = info.getField("packageName")
            val method = utils.getDeclaredMethod("f", String::class.java, List::class.java)
            require(Modifier.isStatic(method.modifiers) && method.returnType == Array<SurfaceControl>::class.java)
            ModernHookRegistry.installCompat("ONextBox-Capture:exclude-floating", method, object : ModernMethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!HideScreenshotFloatingSettings.enabledXposed() || param.throwable != null) return
                    val original = param.result as? Array<*> ?: return
                    val windows = param.args[1] as? List<*> ?: return
                    val merged = original.filterIsInstance<SurfaceControl>().toMutableList()
                    for (window in windows) {
                        if (!info.isInstance(window)) continue
                        if (!ScreenshotFloatingRules.exclude(mode.getInt(window), windowType.getInt(window), pkg.get(window) as? String)) continue
                        val layer = surface.get(window) as? SurfaceControl ?: continue
                        if (layer.isValid && merged.none { it === layer }) merged.add(layer)
                    }
                    // Preserve native exclusions and ownership; never release borrowed surfaces,
                    // set FLAG_SECURE, hide real windows, or bypass capture security checks.
                    if (merged.size != original.size) param.result = merged.toTypedArray()
                }
            })
            // Verified display-capture caller in C17; best-effort deopt for vendor inlining.
            ModernReflect.findClassIfExists("k6.e", loader)?.declaredMethods?.filter { it.name == "n" }?.forEach {
                runCatching { ModernHookRuntime.requireModule().deoptimize(it) }
            }
            HookLog.i("ONextBox-Capture", "Floating-window layer exclusion installed")
        }.onFailure { HookLog.w("ONextBox-Capture", "Floating-window exclusion unavailable; native screenshot retained", it) }
    }
}
