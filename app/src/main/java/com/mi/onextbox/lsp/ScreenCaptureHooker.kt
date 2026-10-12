package com.mi.onextbox.lsp

import android.os.IBinder
import android.view.WindowManager
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernReflect as Reflect
import io.github.libxposed.api.XposedInterface
import java.util.concurrent.atomic.AtomicBoolean

internal object ScreenCaptureHooker {
    private const val TAG = "ONextBox-ScreenCapture"

    fun hookSystemServer(loader: ClassLoader) {
        runCatching {
            val window = Reflect.findClass("com.android.server.wm.WindowState", loader)
            val secure = window.getDeclaredMethod("isSecureLocked").apply { isAccessible = true }
            val attrs = window.getDeclaredField("mAttrs").apply { isAccessible = true }
            val user = window.getDeclaredField("mShowUserId").apply { isAccessible = true }
            val owner = window.getDeclaredMethod("getOwningPackage").apply { isAccessible = true }
            val uid = window.getDeclaredMethod("getOwningUid").apply { isAccessible = true }
            val token = window.getDeclaredMethod("getWindowToken").apply { isAccessible = true }
            val policy = Reflect.findClass("android.app.admin.DevicePolicyCache", loader)
            val getPolicy = policy.getDeclaredMethod("getInstance").apply { isAccessible = true }
            val allowed = policy.getDeclaredMethod("isScreenCaptureAllowed", Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
            val sensitive = Reflect.findClass("com.android.server.wm.SensitiveContentPackages", loader)
                .getDeclaredMethod("shouldBlockScreenCaptureForApp", String::class.java,
                    Int::class.javaPrimitiveType, IBinder::class.java).apply { isAccessible = true }
            val loggedFailure = AtomicBoolean()
            ModernHookRegistry.installFast("screen-capture:window-secure", secure, XposedInterface.Hooker { chain ->
                val original = chain.proceed()
                if (original != true || !ScreenCaptureSettings.enabledXposed()) return@Hooker original
                runCatching {
                    val target = requireNotNull(chain.thisObject)
                    val flags = (attrs.get(target) as WindowManager.LayoutParams).flags
                    if (flags and WindowManager.LayoutParams.FLAG_SECURE == 0) return@runCatching original
                    val wm = requireNotNull(Reflect.getObjectField(target, "mWmService"))
                    val packages = requireNotNull(Reflect.getObjectField(wm, "mSensitiveContentPackages"))
                    val policyAllows = allowed.invoke(getPolicy.invoke(null), user.getInt(target)) == true
                    val blocked = sensitive.invoke(packages, owner.invoke(target), uid.invoke(target), token.invoke(target)) != false
                    if (ScreenCaptureRules.canOverride(true, flags, policyAllows, blocked)) false else original
                }.getOrElse {
                    if (loggedFailure.compareAndSet(false, true)) HookLog.w(TAG, "Keeping secure window policy after compatibility failure", it)
                    original
                }
            })
            HookLog.i(TAG, "Window capture hook ready; opt-in setting, device policy and consent retained")
        }.onFailure { HookLog.w(TAG, "Window capture hook unavailable; stock protection retained", it) }
    }
}
