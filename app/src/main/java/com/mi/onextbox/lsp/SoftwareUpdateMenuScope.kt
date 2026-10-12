package com.mi.onextbox.lsp

internal class SoftwareUpdateMenuScope {
    private val active = ThreadLocal<Boolean>()

    fun overrides(key: String?): Boolean = active.get() == true && key == "development_settings_enabled"

    fun <T> enter(enabled: Boolean, action: () -> T): T {
        val previous = active.get()
        active.set(enabled)
        return try { action() } finally {
            if (previous == null) active.remove() else active.set(previous)
        }
    }
}
