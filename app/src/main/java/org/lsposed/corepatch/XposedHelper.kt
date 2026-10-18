// API 102 / ONextBox lifecycle adapter for LSPosed/CorePatch (GPL v2.0).
package org.lsposed.corepatch

import android.annotation.SuppressLint
import android.graphics.Point
import com.mi.onextbox.lsp.HookLog
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernMethodHook
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method

internal typealias BeforeCallback = (XposedHelper.BeforeHookCallback) -> Unit
internal typealias AfterCallback = (XposedHelper.AfterHookCallback) -> Unit

internal object XposedHelper {
    lateinit var hostClassLoader: ClassLoader
        private set
    private val fieldOffsetValue by lazy { getFieldOffsetOffset() }
    fun setHostClassLoader(loader: ClassLoader) { hostClassLoader = loader }

    class BeforeHookCallback(private val param: ModernMethodHook.MethodHookParam) {
        val thisObject get() = param.thisObject
        val args get() = param.args
        fun returnAndSkip(result: Any?) { param.result = result }
    }
    class AfterHookCallback(private val param: ModernMethodHook.MethodHookParam) {
        val thisObject get() = param.thisObject
        val args get() = param.args
        var result: Any?
            get() = param.result
            set(value) { param.result = value }
        var throwable: Throwable?
            get() = param.throwable
            set(value) { param.throwable = value }
    }
    fun hookBefore(member: Executable, callback: BeforeCallback): XposedInterface.HookHandle =
        ModernHookRegistry.installCompat("corepatch:before:$member", member, object : ModernMethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) { callback(BeforeHookCallback(param)) }
        })
    fun hookAfter(member: Executable, callback: AfterCallback): XposedInterface.HookHandle =
        ModernHookRegistry.installCompat("corepatch:after:$member", member, object : ModernMethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) { callback(AfterHookCallback(param)) }
        })
    fun log(message: String, throwable: Throwable? = null) {
        if (throwable == null) HookLog.i("ONextBox-CorePatch", message)
        else HookLog.w("ONextBox-CorePatch", message, throwable)
    }
    fun deoptimize(method: Method) = ModernHookRuntime.requireModule().deoptimize(method)
    fun getOriginInvoker(method: Method) =
        ModernHookRuntime.requireModule().getInvoker(method).setType(XposedInterface.Invoker.Type.ORIGIN)
    fun findClassIfExists(name: String): Class<*>? =
        try { hostClassLoader.loadClass(name) } catch (_: ClassNotFoundException) { null }

    fun setStaticBoolean(field: Field, value: Boolean) {
        try {
            // Resolve the target field before reading its internal ART offset.
            field.isAccessible = true
            field.get(null)
        } catch (_: IllegalAccessException) {
        }

        val offset = UnsafeAccess.getInt(field, fieldOffsetValue).toLong()
        UnsafeAccess.putBoolean(field.declaringClass, offset, value)
    }

    @SuppressLint("DiscouragedPrivateApi")
    private object UnsafeAccess {
        private val unsafeClass = Class.forName("sun.misc.Unsafe")
        private val unsafeInstance = unsafeClass.getDeclaredField("theUnsafe").let { field ->
            field.isAccessible = true
            field.get(null)
        }
        private val getIntMethod = unsafeClass.getMethod(
            "getInt", Any::class.java, Long::class.javaPrimitiveType
        )
        private val putIntMethod = unsafeClass.getMethod(
            "putInt", Any::class.java, Long::class.javaPrimitiveType, Int::class.javaPrimitiveType
        )
        private val putBooleanMethod = unsafeClass.getMethod(
            "putBoolean",
            Any::class.java,
            Long::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        )
        private val objectFieldOffsetMethod = unsafeClass.getMethod(
            "objectFieldOffset", Field::class.java
        )

        fun getInt(target: Any, offset: Long) =
            getIntMethod.invoke(unsafeInstance, target, offset) as Int

        fun putInt(target: Any, offset: Long, value: Int) {
            putIntMethod.invoke(unsafeInstance, target, offset, value)
        }

        fun putBoolean(target: Any, offset: Long, value: Boolean) {
            putBooleanMethod.invoke(unsafeInstance, target, offset, value)
        }

        fun objectFieldOffset(field: Field) =
            objectFieldOffsetMethod.invoke(unsafeInstance, field) as Long
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SoonBlockedPrivateApi")
    private fun getFieldOffsetOffset(): Long {
        var noSuchFieldException: NoSuchFieldException? = null
        try {
            val offsetField = Field::class.java.getDeclaredField("offset")
            offsetField.isAccessible = true
            offsetField.getInt(offsetField)
            return UnsafeAccess.objectFieldOffset(offsetField)
        } catch (e: NoSuchFieldException) {
            noSuchFieldException = e
        } catch (_: IllegalAccessException) {
        } catch (_: UnsupportedOperationException) {
        }

        val probeField = Point::class.java.getDeclaredField("x")
        probeField.getInt(Point())
        val fieldOffset = UnsafeAccess.objectFieldOffset(probeField).toInt()
        for (offset in 8 until 256 step 4) {
            val offsetLong = offset.toLong()
            if (UnsafeAccess.getInt(probeField, offsetLong) != fieldOffset) continue

            val modifiedOffset = fieldOffset.inv()
            UnsafeAccess.putInt(probeField, offsetLong, modifiedOffset)
            val currentOffset = UnsafeAccess.objectFieldOffset(probeField).toInt()
            UnsafeAccess.putInt(probeField, offsetLong, fieldOffset)
            if (currentOffset == modifiedOffset) return offsetLong
        }
        throw noSuchFieldException ?: NoSuchFieldException("Field.offset")
    }
}
