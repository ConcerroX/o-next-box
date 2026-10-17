package com.mi.onextbox.lsp

import com.mi.onextbox.lsp.LspConfig.KeyguardFeature
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect as Reflect
import java.lang.reflect.Method

internal object PasswordDialHighlightHooker {
    private const val TAG = "ONextBox-PasswordDialLight"
    private const val EFFECT_PARAMS = "com.coui.appcompat.passworddial.olivelink.PasswordDialEffectSwitchParams"
    private const val MATERIAL_HELPER = "com.coui.appcompat.passworddial.material.PasswordDialKeyMaterialHelper"
    private const val ENV_LIGHT_HELPER = "com.coui.appcompat.passworddial.material.PasswordDialKeyEnvLightHelper"
    private const val BLUR_HOLDER = "com.coui.appcompat.passworddial.material.PasswordDialKeyBlurHolder"
    private val getters = setOf("getEnvLightEnabled", "getOpticsEnabled")
    private val callers = setOf("isEnvLightPressActive", "isPressOpticsActive", "applyPressLightFactor",
        "dispatchTouchEvent", "syncEnvLightFromMaterialState", "refreshReceiver", "createOpticsParams",
        "setOpticsAlpha")

    private fun enabled() = LspConfig.isKeyguardFeatureEnabledXposed(KeyguardFeature.BlockPasswordHighlight)

    fun hook(loader: ClassLoader) {
        if (!enabled()) return
        runCatching { install(loader) }
            .onSuccess { HookLog.i(TAG, "Installed password dial highlight gate") }
            .onFailure { HookLog.w(TAG, "Unavailable password dial highlight, stock glow retained", it) }
    }

    private fun install(loader: ClassLoader) {
        val params = Reflect.findClass(EFFECT_PARAMS, loader)
        val helpers = listOf(MATERIAL_HELPER, ENV_LIGHT_HELPER, BLUR_HOLDER).flatMap {
            Reflect.findClass(it, loader).declaredMethods.toList()
        }
        val gates = params.declaredMethods.filter { it.name in getters && it.parameterTypes.isEmpty() &&
            it.returnType == Boolean::class.javaPrimitiveType }
        val stroke = helpers.single { it.name == "applyAllParams" && it.parameterTypes.size == 2 &&
            it.parameterTypes[1] == Boolean::class.javaPrimitiveType && it.returnType == Void.TYPE }
        check(gates.size == getters.size) { "Unknown password dial effect switches" }
        val callerTargets = helpers.filter { it.name in callers }
        check(callerTargets.isNotEmpty()) { "No password dial effect caller resolved" }
        (gates + stroke + callerTargets).forEach(::deoptimize)
        gates.forEach { gate ->
            ModernHookRegistry.installCompat("$TAG:${gate.name}", gate, object : ModernMethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (enabled()) param.result = false
                }
            })
        }
        ModernHookRegistry.installCompat("$TAG:stroke", stroke, object : ModernMethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (enabled()) param.args[1] = false
            }
        })
    }

    private fun deoptimize(method: Method) {
        if (!ModernHookRuntime.requireModule().deoptimize(method)) {
            HookLog.w(TAG, "Deoptimization unavailable for ${method.declaringClass.name}#${method.name}")
        }
    }
}
