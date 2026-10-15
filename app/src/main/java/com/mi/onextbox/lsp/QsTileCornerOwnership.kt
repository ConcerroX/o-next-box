package com.mi.onextbox.lsp

/** Equal native shapes need not be the same object; track the provider actually accepted. */
internal data class QsTileCornerOwnership<T : Any>(val original: T, val custom: T, val radius: Float, val weight: Float? = null) {
    fun matches(current: T, nextRadius: Float, nextWeight: Float? = weight) =
        current === custom && radius == nextRadius && weight == nextWeight
    fun originalFor(current: T): T = if (current === custom) original else current
    fun shouldRestore(current: T) = current === custom
    fun accepted(current: T) = copy(custom = current)
}
