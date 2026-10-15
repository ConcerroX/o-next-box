package com.mi.onextbox.lsp

/** Equal native shapes need not be the same object; track the provider actually accepted. */
internal data class QsTileCornerOwnership<T : Any>(val original: T, val custom: T, val radius: Float) {
    fun matches(current: T, nextRadius: Float) = current === custom && radius == nextRadius
    fun originalFor(current: T): T = if (current === custom) original else current
    fun shouldRestore(current: T) = current === custom
    fun accepted(current: T) = copy(custom = current)
}
