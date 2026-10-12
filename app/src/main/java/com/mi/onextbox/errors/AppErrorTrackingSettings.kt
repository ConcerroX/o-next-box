package com.mi.onextbox.errors

import android.content.Context
import com.mi.onextbox.lsp.LspPreferenceStore
import com.mi.onextbox.lsp.ModernXposedPreferenceSync

object AppErrorTrackingSettings {
    const val KEY = "app_error_tracking_enabled"
    fun isEnabled(context: Context): Boolean = LspPreferenceStore.readBoolean(context, KEY, false)
    fun setEnabled(context: Context, enabled: Boolean) = ModernXposedPreferenceSync.setBooleanAndSync(context, KEY, enabled)
}
