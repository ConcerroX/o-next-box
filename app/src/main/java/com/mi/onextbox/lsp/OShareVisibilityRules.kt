package com.mi.onextbox.lsp

internal object OShareVisibilityRules {
    const val TIMEOUT_ACTION = "oplus.action.oshare.switch.timeout"

    fun suppress(enabled: Boolean, action: String?): Boolean = enabled && action == TIMEOUT_ACTION

    fun unlimitedLabel(label: String): String =
        label.replace(Regex("\\s*[（(][^（）()]*[）)]\\s*$"), "").trim().ifBlank { label }

    fun runCountdown(enabled: Boolean, clear: () -> Unit, original: () -> Any?, failure: (Throwable) -> Unit): Any? {
        if (!enabled) return original()
        return try {
            clear()
            null
        } catch (error: Exception) {
            failure(error)
            original()
        }
    }
}
