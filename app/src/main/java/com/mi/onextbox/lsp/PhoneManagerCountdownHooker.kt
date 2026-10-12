package com.mi.onextbox.lsp

import android.os.CountDownTimer
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import io.github.libxposed.api.XposedInterface
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal object PhoneManagerCountdownHooker {
    private const val TAG = "ONextBox-PhoneManager"
    private const val OWNER = "com.oplus.phonemanager.common.DialogCrossActivity"
    private const val DATA = "com.oplus.phonemanager.common.entity.DialogCrossData"

    fun hook(loader: ClassLoader, apkPath: String) {
        val key = "$TAG:countdown@${System.identityHashCode(loader)}"
        runCatching {
            val target = resolve(loader, apkPath)
            ModernHookRegistry.installFast(key, target, XposedInterface.Hooker { chain ->
                val timer = chain.thisObject as? CountDownTimer
                if (timer == null) {
                    chain.proceed()
                } else {
                    PhoneManagerCountdownRules.tick(
                        enabled = PhoneManagerSettings.isEnabledXposed(),
                        complete = { timer.onFinish() },
                        cancel = { timer.cancel() },
                        failure = { HookLog.w(TAG, "Unable to finish dialog countdown", it) },
                        original = { chain.proceed() },
                    )
                }
            })
            HookLog.i(TAG, "Dialog countdown hook installed: ${target.declaringClass.name}")
        }.onFailure {
            ModernHookRegistry.unhook(key)
            HookLog.w(TAG, "Dialog countdown hook unavailable; stock behavior retained", it)
        }
    }

    private fun resolve(loader: ClassLoader, apkPath: String): Method {
        val owner = Class.forName(OWNER, false, loader)
        val data = Class.forName(DATA, false, loader)
        val initializer = owner.getDeclaredMethod("initCountdown", data)
        require(initializer.returnType == Void.TYPE && !Modifier.isStatic(initializer.modifiers))
        val direct = runCatching {
            validate(Class.forName("$OWNER\$initCountdown\$2", false, loader), owner, data)
        }.getOrNull()
        if (direct != null) return direct
        System.loadLibrary("dexkit")
        return DexKitBridge.create(apkPath).use { dex ->
            val init = dex.findMethod {
                matcher {
                    declaredClass = OWNER
                    name = "initCountdown"
                    returnType = "void"
                    paramTypes(DATA)
                }
            }.single()
            init.invokes.asSequence()
                .filter { it.name == "<init>" && it.className.startsWith("$OWNER\$") }
                .mapNotNull {
                    runCatching { validate(Class.forName(it.className, false, loader), owner, data) }.getOrNull()
                }
                .distinct()
                .single()
        }
    }

    private fun validate(timer: Class<*>, owner: Class<*>, data: Class<*>): Method {
        require(timer.superclass == CountDownTimer::class.java)
        require(timer.declaredFields.any { it.type == owner && !Modifier.isStatic(it.modifiers) })
        require(timer.declaredConstructors.any { constructor ->
            constructor.parameterTypes.contains(owner) && constructor.parameterTypes.contains(data)
        })
        val finish = timer.getDeclaredMethod("onFinish")
        require(finish.returnType == Void.TYPE && !Modifier.isStatic(finish.modifiers))
        return timer.getDeclaredMethod("onTick", Long::class.javaPrimitiveType).also {
            require(it.returnType == Void.TYPE && !Modifier.isStatic(it.modifiers))
        }
    }
}
