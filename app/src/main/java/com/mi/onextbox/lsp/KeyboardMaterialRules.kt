package com.mi.onextbox.lsp

internal object KeyboardMaterialRules {
    const val PACKAGE_NAME = "com.oplus.keyboard"
    val stockPackages = setOf(
        "com.heytap.quicksearchbox", "com.heytap.speechassist", "com.android.contacts",
        "com.android.settings", "com.heytap.market", "com.oplus.aimemory",
        "com.coloros.filemanager", "com.oplus.tips",
    )
    private val packageName = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")

    fun isMaterialAllowlist(values: Collection<*>): Boolean =
        values.size >= stockPackages.size && values.all { it is String && packageName.matches(it) } &&
            values.toSet().containsAll(stockPackages)

    fun shouldAllow(enabled: Boolean, isMaterialList: Boolean, candidate: Any?): Boolean =
        enabled && isMaterialList && candidate is String && candidate.length <= 255 && packageName.matches(candidate)
}
