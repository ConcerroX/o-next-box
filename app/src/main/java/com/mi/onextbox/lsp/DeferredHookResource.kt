package com.mi.onextbox.lsp

/** One-shot, caller-thread initialization. A failed ABI probe is never retried in a hot path. */
internal class DeferredHookResource<T : Any>(private val create: () -> T) {
    private var resource: T? = null
    private var failed = false

    @Synchronized
    fun prepare(onFailure: (Throwable) -> Unit): Boolean {
        if (resource != null) return true
        if (failed) return false
        return try {
            resource = create()
            true
        } catch (error: Throwable) {
            failed = true
            onFailure(error)
            false
        }
    }

    @Synchronized
    fun value(): T = checkNotNull(resource)
}
