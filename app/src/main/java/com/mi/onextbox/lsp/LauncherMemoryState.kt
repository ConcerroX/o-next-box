package com.mi.onextbox.lsp

import java.lang.reflect.Field
import java.util.WeakHashMap

internal class LauncherMemoryState(private val allowed: Field, private val detail: Field) {
    private val originals = WeakHashMap<Any, Boolean>()

    init {
        require(allowed.type == Boolean::class.javaPrimitiveType && detail.type == allowed.type)
        allowed.isAccessible = true
        detail.isAccessible = true
    }

    @Synchronized
    fun force(owner: Any) {
        if (!originals.containsKey(owner)) originals[owner] = allowed.getBoolean(owner)
        if (!allowed.getBoolean(owner)) allowed.setBoolean(owner, true)
        if (!detail.getBoolean(owner)) detail.setBoolean(owner, true)
    }

    @Synchronized
    fun restoreAllowed(owner: Any): Boolean {
        val original = originals[owner] ?: return false
        allowed.setBoolean(owner, original)
        originals.remove(owner)
        return true
    }

    @Synchronized
    fun export(): Array<Array<Any>> = originals.map { (owner, original) -> arrayOf(owner, original) }.toTypedArray()

    @Synchronized
    fun import(rows: Array<*>) {
        rows.forEach { value ->
            val row = value as? Array<*> ?: return@forEach
            val owner = row.getOrNull(0) ?: return@forEach
            val original = row.getOrNull(1) as? Boolean ?: return@forEach
            if (allowed.declaringClass.isInstance(owner)) originals.putIfAbsent(owner, original)
        }
    }
}
