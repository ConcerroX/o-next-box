package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.HookConfigSnapshot

internal object LauncherGestureSettings {
    // Preserve the saved switch while narrowing it to native secondary-menu grouping.
    const val MENU = "launcher_disable_icon_menu"
    const val TOP = "launcher_simplify_swipe_top"
    const val WIDTH = "launcher_swipe_float_width"
    fun menu(context: Context) = LspPreferenceStore.prefs(context).getBoolean(MENU, false)
    fun top(context: Context) = LspPreferenceStore.prefs(context).getBoolean(TOP, false)
    fun width(context: Context) = LauncherGestureRules.width(LspPreferenceStore.prefs(context).getInt(WIDTH, 100))
    fun menuXposed() = HookConfigSnapshot.boolean(MENU, false) == true
    fun topXposed() = HookConfigSnapshot.boolean(TOP, false) == true
    fun widthXposed() = LauncherGestureRules.width(HookConfigSnapshot.int(WIDTH, 100) ?: 100)
    fun save(context: Context, key: String, enabled: Boolean) {
        require(key == MENU || key == TOP)
        check(LspPreferenceStore.prefs(context).edit().putBoolean(key, enabled).commit())
        LspConfig.syncReadableState(context)
    }
    fun saveWidth(context: Context, value: Int) {
        check(LspPreferenceStore.prefs(context).edit().putInt(WIDTH, LauncherGestureRules.width(value)).commit())
        LspConfig.syncReadableState(context)
    }
}

internal object LauncherGestureRules {
    fun width(value: Int) = value.coerceIn(50, 100)
    fun secondaryMenuGroupingCount(disabled: Boolean, original: Int) = if (disabled) 0 else original
    fun blockSplit(enabled: Boolean, option: String?) = enabled && option == "SPLIT_WINDOW"
    fun selectAxis(original: Int, floatingAxis: Int, offset: Int, screenWidth: Int, rotation: Int, percent: Int): Int {
        if (original and 0xf0 != 0x30 || floatingAxis and 0xf0 != 0x30 || screenWidth <= 0 || rotation !in 0..3) return original
        val boundary = screenWidth * (0.5f - width(percent) / 100f)
        val inFloatingArea = if (rotation == 3) offset <= -boundary else offset >= boundary
        return if (inFloatingArea) floatingAxis else original
    }
}
