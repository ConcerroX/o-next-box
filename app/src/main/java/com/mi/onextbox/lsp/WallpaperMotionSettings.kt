package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal object WallpaperMotionSettings {
    const val ENGINE_PACKAGE = "com.heytap.colorfulengine"
    const val ENABLED = "wallpaper_motion_enabled"
    const val AMOUNT = "wallpaper_motion_percent"
    fun enabled(context: Context) = LspPreferenceStore.prefs(context).getBoolean(ENABLED, false)
    fun amount(context: Context) = normalize(LspPreferenceStore.prefs(context).getInt(AMOUNT, 100))
    fun normalize(value: Int) = value.coerceIn(50, 200)
    fun scaleGyro(value: Float, percent: Int): Float =
        if (!value.isFinite()) value else value * (normalize(percent) / 100f)
    fun scale(original: Float, enabled: Boolean, percent: Int): Float =
        if (!enabled || !original.isFinite() || original <= 0f) original
        else original * normalize(percent) / 100f
    fun percentXposed() = if (HookConfigSnapshot.boolean(ENABLED, false) == true)
        normalize(HookConfigSnapshot.int(AMOUNT, 100) ?: 100) else 100
    fun save(context: Context, enabled: Boolean, amount: Int) {
        check(LspPreferenceStore.prefs(context).edit().putBoolean(ENABLED, enabled)
            .putInt(AMOUNT, normalize(amount)).commit())
        LspConfig.syncReadableState(context)
    }
}
