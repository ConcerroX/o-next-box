package com.mi.onextbox.lsp

internal object NotificationClearButtonRules {
    fun width(enabled: Boolean, stockWidth: Int, height: Int): Int =
        if (enabled && stockWidth > 0 && height > 0) height else stockWidth

    fun restoreWidth(current: Int, stock: Int, applied: Int): Int =
        if (current == applied && stock > 0) stock else current
}
