package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect

internal object RotationSuggestionSettings {
    const val KEY = "systemui_hide_rotation_suggestion"
    fun enabled(context: Context) = LspPreferenceStore.prefs(context).getBoolean(KEY, false)
    fun enabledXposed() = HookConfigSnapshot.boolean(KEY, false) == true
    fun set(context: Context, value: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(KEY, value).commit())
        LspConfig.syncReadableState(context)
    }
}

internal object RotationSuggestionHooker {
    private const val TAG = "ONextBox-RotationSuggestion"
    fun hook(loader: ClassLoader) {
        runCatching {
            val type = ModernReflect.findClass("com.android.systemui.shared.rotation.RotationButtonController", loader)
            val method = type.getDeclaredMethod("setRotateSuggestionButtonState", Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType)
            require(method.returnType == Void.TYPE)
            ModernHookRegistry.installCompat("$TAG:visibility", method, object : ModernMethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (RotationSuggestionSettings.enabledXposed()) {
                        // Keep native hide/animation cleanup instead of dropping the entire callback.
                        param.args[0] = false
                        param.args[1] = true
                    }
                }
            })
            // The vendor inlines FloatingRotationButton.show into this controller.
            // Deoptimize its local callers rather than intercepting global orientation policy.
            type.declaredMethods.forEach {
                runCatching { ModernHookRuntime.requireModule().deoptimize(it) }
                    .onFailure { error -> HookLog.w(TAG, "Rotation UI deoptimization unavailable: ${it.name}", error) }
            }
            HookLog.i(TAG, "Rotation suggestion visibility hook installed")
        }.onFailure { HookLog.w(TAG, "Rotation suggestion adapter unavailable; stock behavior retained", it) }
    }
}
