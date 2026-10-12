package com.mi.onextbox.lsp

internal class SoftwareUpdateBootloaderScope {
    private val remaining = ThreadLocal<Int>()

    fun consumeOverride(): Boolean {
        val count = remaining.get() ?: return false
        if (count <= 0) return false
        remaining.set(count - 1)
        return true
    }

    fun <T> enter(enabled: Boolean, action: () -> T): T {
        val previous = remaining.get()
        remaining.set(if (enabled) 1 else 0)
        return try { action() } finally {
            if (previous == null) remaining.remove() else remaining.set(previous)
        }
    }
}
