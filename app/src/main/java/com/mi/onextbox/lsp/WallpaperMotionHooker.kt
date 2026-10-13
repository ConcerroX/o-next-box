package com.mi.onextbox.lsp

import android.hardware.Sensor
import android.hardware.SensorEvent
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/** Scope native input to handles that loaded a 3D mesh; never change shared sensor events. */
internal object WallpaperMotionHooker {
    private const val TAG = "ONextBox-3D"
    fun hook(loader: ClassLoader?) {
        val engine = ModernReflect.findClassIfExists("com.oplus.vfxsdk.naive.coe.engine.NativeEngine", loader) ?: return
        val message = ModernReflect.findClassIfExists("com.oplus.vfxsdk.naive.coe.engine.Message", loader) ?: return
        runCatching {
            val name = message.getMethod("getName")
            val eventConstructor = SensorEvent::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
            val eventFields = SensorEvent::class.java.fields.filter {
                !Modifier.isStatic(it.modifiers) && !Modifier.isFinal(it.modifiers) && it.name != "values"
            }
            engine.getDeclaredMethod("nativeSendMessage", Long::class.javaPrimitiveType, message)
            engine.getDeclaredMethod("nativeSensorEvent", Long::class.javaPrimitiveType, SensorEvent::class.java)
            engine.getDeclaredMethod("nativeDestroyEngine", Long::class.javaPrimitiveType)
            val handles = ConcurrentHashMap<Long, Int>()
            ModernReflect.findAndHookMethod(engine, "nativeSendMessage", Long::class.javaPrimitiveType, message,
                object : ModernMethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.throwable != null) return
                        val key = name.invoke(param.args[1]) as? String ?: return
                        if (key != "loadWallpaper3DData" && key != "loadWallpaper3DDataMeshDepth") return
                        val handle = param.args[0] as Long
                        if (handle != 0L && handles.putIfAbsent(handle, -1) == null)
                            HookLog.i(TAG, "3D resource load reached native engine: $key")
                    }
                })
            ModernReflect.findAndHookMethod(engine, "nativeSensorEvent", Long::class.javaPrimitiveType, SensorEvent::class.java,
                object : ModernMethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val handle = param.args[0] as Long
                        if (!handles.containsKey(handle)) return
                        val event = param.args[1] as? SensorEvent ?: return
                        if (event.sensor?.type != Sensor.TYPE_GYROSCOPE) return
                        val percent = WallpaperMotionSettings.percentXposed()
                        if (handles.put(handle, percent) != percent)
                            HookLog.i(TAG, "3D gyroscope input reached native engine; sensitivity=$percent%")
                        if (percent == 100 || event.values.size < 3) return
                        val copy = eventConstructor.newInstance(event.values.size)
                        eventFields.forEach { it.set(copy, it.get(event)) }
                        event.values.copyInto(copy.values)
                        for (index in 0..2) copy.values[index] = WallpaperMotionSettings.scaleGyro(event.values[index], percent)
                        param.args[1] = copy
                    }
                })
            ModernReflect.findAndHookMethod(engine, "nativeDestroyEngine", Long::class.javaPrimitiveType,
                object : ModernMethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) { handles.remove(param.args[0] as Long) }
                })
            HookLog.i(TAG, "Native 3D input hooks installed; original angle limits preserved")
        }.onFailure { HookLog.w(TAG, "3D input hooks unavailable", it) }
    }
}
