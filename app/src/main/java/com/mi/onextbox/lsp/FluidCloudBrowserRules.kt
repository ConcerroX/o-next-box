package com.mi.onextbox.lsp

internal object FluidCloudBrowserRules {
    const val STOCK_BROWSER = "com.heytap.browser"

    fun firstAvailableBrowser(vararg lookups: () -> String?): String? {
        for (lookup in lookups) {
            val candidate = runCatching { lookup() }.getOrNull()
            if (!candidate.isNullOrBlank() && candidate != "android") return candidate
        }
        return null
    }

    fun accepts(
        enabled: Boolean,
        textType: Int?,
        intentType: Int,
        packageName: String?,
        action: String?,
        scheme: String?,
        host: String?,
        explicitPackage: String?,
        componentPackage: String?,
        hasSelector: Boolean,
    ): Boolean = enabled && textType == 2 && intentType == 1 && packageName == STOCK_BROWSER &&
        action == "android.intent.action.VIEW" &&
        (scheme.equals("http", true) || scheme.equals("https", true)) && !host.isNullOrBlank() &&
        (explicitPackage == null || explicitPackage == STOCK_BROWSER) &&
        (componentPackage == null || componentPackage == STOCK_BROWSER) && !hasSelector

    fun <T> withTarget(
        originalPackage: String?,
        targetPackage: String,
        setPackage: (String?) -> Unit,
        launch: () -> T,
    ): T {
        try {
            setPackage(targetPackage)
            return launch()
        } finally {
            setPackage(originalPackage)
        }
    }
}
