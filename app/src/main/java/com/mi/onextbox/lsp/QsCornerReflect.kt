package com.mi.onextbox.lsp

import com.mi.onextbox.lsp.compat.ModernReflect
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/** Feature-local hot getter/float-setter cache; never changes reflection for other hooks. */
internal object QsCornerReflect {
    private data class Key(val type: Class<*>, val name: String, val floatArg: Boolean)
    private val methods = ConcurrentHashMap<Key, Method>()

    fun callMethod(target: Any?, name: String, vararg args: Any?): Any? {
        val receiver = requireNotNull(target)
        if (args.isNotEmpty() && !(args.size == 1 && args[0] is Float))
            return ModernReflect.callMethod(receiver, name, *args)
        val key = Key(receiver.javaClass, name, args.isNotEmpty())
        val method = methods.getOrPut(key) {
            ModernReflect.findMethodExact(key.type, name,
                if (key.floatArg) arrayOf(Float::class.javaPrimitiveType!!) else emptyArray())
        }
        return method.invoke(receiver, *args)
    }
    fun getObjectField(target: Any, name: String) = ModernReflect.getObjectField(target, name)
    fun setObjectField(target: Any, name: String, value: Any?) = ModernReflect.setObjectField(target, name, value)
    fun findClassIfExists(name: String, loader: ClassLoader?) = ModernReflect.findClassIfExists(name, loader)
    fun newInstance(type: Class<*>, vararg args: Any?) = ModernReflect.newInstance(type, *args)
}
