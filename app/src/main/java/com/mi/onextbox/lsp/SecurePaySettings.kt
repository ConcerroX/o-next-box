package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal object SecurePaySettings {
    const val PACKAGE_NAME = "com.coloros.securepay"
    const val HIDE_RISK_CLOUD = "secure_pay_hide_risk_cloud"

    fun enabled(context: Context): Boolean =
        LspPreferenceStore.prefs(context).getBoolean(HIDE_RISK_CLOUD, false)

    fun enabledXposed(): Boolean = HookConfigSnapshot.boolean(HIDE_RISK_CLOUD, false) == true

    fun setEnabled(context: Context, enabled: Boolean) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(HIDE_RISK_CLOUD, enabled).commit())
        LspConfig.syncReadableState(context)
    }
}
