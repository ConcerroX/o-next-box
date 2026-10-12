package com.mi.onextbox.lsp

/** Only remove an application's window flag, not device-policy or sensitive-content protection. */
internal object ScreenCaptureRules {
    fun canOverride(enabled: Boolean, flags: Int, policyAllows: Boolean, sensitive: Boolean): Boolean =
        enabled && flags and 0x2000 != 0 && policyAllows && !sensitive
}
