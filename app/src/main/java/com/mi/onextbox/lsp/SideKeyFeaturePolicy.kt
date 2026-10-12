package com.mi.onextbox.lsp

internal object SideKeyFeaturePolicy {
    val available: Boolean = false

    fun enabled(requested: Boolean): Boolean = available && requested

    fun normalize(state: SideKeyState): SideKeyState = state.copy(enabled = enabled(state.enabled))
}
