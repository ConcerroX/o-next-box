package com.mi.onextbox.lsp

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

internal class AospBackPanelGesture(val requested: Boolean) {
    var usingNative = false
        private set
    private var accepted = false

    fun start(show: () -> Unit): Boolean {
        if (!requested || usingNative) return usingNative
        show()
        usingNative = true
        return true
    }

    fun finish(stockState: Int) {
        accepted = stockState == 2
    }

    fun terminalAction(action: Int): Int = if (action == 1 && accepted) 1 else 3

    fun fail() {
        usingNative = false
    }
}

internal object AospBackPanelRules {
    fun acceptsInput(enabled: Boolean, touchscreen: Boolean, pointers: Int, finger: Boolean): Boolean =
        enabled && touchscreen && pointers == 1 && finger

    fun canReload(activeGestures: Int, allGone: Boolean, now: Long, lastEvent: Long): Boolean =
        activeGestures == 0 && allGone && now >= lastEvent && now - lastEvent >= 700L
}

internal object AospBackPanelVisualCallback : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? = when (method.name) {
        "equals" -> proxy === args?.getOrNull(0)
        "hashCode" -> System.identityHashCode(proxy)
        "toString" -> "ONextBox-AospBackPanelVisualCallback"
        else -> null
    }
}
