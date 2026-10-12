package com.mi.onextbox.lsp

internal object KeyboardDismissButtonRules {
    fun stockVisibility(current: Int, previous: Int?, nativeUpdated: Boolean): Int =
        if (nativeUpdated || current != 4) current else previous ?: current

    fun visibility(enabled: Boolean, packageName: String, gestural: Boolean, stock: Int): Int =
        if (enabled && packageName == KeyboardMaterialRules.PACKAGE_NAME && gestural && stock == 0) 4 else stock
}
