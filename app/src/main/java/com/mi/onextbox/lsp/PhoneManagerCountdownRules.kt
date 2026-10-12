package com.mi.onextbox.lsp

internal object PhoneManagerCountdownRules {
    fun tick(
        enabled: Boolean,
        complete: () -> Unit,
        cancel: () -> Unit,
        failure: (Throwable) -> Unit,
        original: () -> Any?,
    ): Any? {
        if (!enabled) return original()
        try {
            complete()
        } catch (error: Throwable) {
            failure(error)
            return original()
        }
        cancel()
        return null
    }
}
