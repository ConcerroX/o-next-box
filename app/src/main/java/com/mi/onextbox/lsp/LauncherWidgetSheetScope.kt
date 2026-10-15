package com.mi.onextbox.lsp

import java.util.WeakHashMap

/** Ownership survives a switch change while an already-created menu/panel is still visible. */
internal class LauncherWidgetSheetScope {
    private val actions = WeakHashMap<Any, Boolean>()
    private val sheets = WeakHashMap<Any, Boolean>()
    private val opening = ThreadLocal<MutableList<Any>?>()
    fun recordAction(action: Any) { synchronized(actions) { actions[action] = true } }
    fun ownsAction(action: Any?) = synchronized(actions) { actions[action] == true }
    fun ownsSheet(sheet: Any?) = synchronized(sheets) { sheets[sheet] == true }
    fun recordOpeningSheet(sheet: Any): Boolean {
        val current = opening.get() ?: return false
        synchronized(sheets) { sheets[sheet] = true }
        current.add(sheet)
        return true
    }
    fun openingSheets(): List<Any> = opening.get()?.toList().orEmpty()
    fun <T> duringAction(block: () -> T): T {
        val previous = opening.get()
        opening.set(mutableListOf())
        return try { block() } finally {
            if (previous == null) opening.remove() else opening.set(previous)
        }
    }
}
