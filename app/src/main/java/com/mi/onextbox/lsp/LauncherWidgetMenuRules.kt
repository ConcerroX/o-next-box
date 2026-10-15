package com.mi.onextbox.lsp

internal object LauncherWidgetMenuRules {
    /** App icons only: not folders, widgets, pinned deep shortcuts or install placeholders. */
    fun eligible(itemType: Int, disabled: Boolean, promise: Boolean, archived: Boolean, nonInstalled: Boolean): Boolean =
        itemType == 0 && !disabled && !promise && !archived && !nonInstalled

    /** Preserve every native factory and its position, including unrelated duplicates. */
    fun <T> appendFactory(factories: List<T>, widgets: T): List<T> =
        if (factories.any { it === widgets }) factories else factories + widgets
}
