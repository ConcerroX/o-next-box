package com.mi.onextbox.lsp

import android.content.Context
import android.provider.Settings
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect

/** Keep native default high refresh separate from the newly exposed extreme choice. */
internal object SettingsRefreshRateSelectionHooker {
    private const val TAG = "SettingsRefreshRate"

    fun hook(loader: ClassLoader, utils: Class<*>) {
        val controller = ModernReflect.findClass(
            "com.oplus.settings.feature.display.ScreenRefreshRateController", loader,
        )
        val legacy144 = ModernReflect.findDeclaredMethodExact(
            controller, "get144hzRateMode", Context::class.java, Int::class.javaPrimitiveType!!,
        )
        val choice = ModernReflect.findDeclaredMethodExact(utils, "getChoiceModeRate", Context::class.java)
        check(legacy144.returnType == Int::class.javaPrimitiveType && choice.returnType == Int::class.javaPrimitiveType)
        val prefix = "settings-refresh-selection:${System.identityHashCode(loader)}:"
        try {
            ModernHookRegistry.installCompat(prefix + "default-high", legacy144, object : ModernMethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val requestedMode = param.args.getOrNull(1) as? Int ?: return
                    if (SettingsRefreshRateSelectionRules.keepDefaultHighMode(
                            LspConfig.isSettingsRefreshRateUnlockedXposed(), requestedMode,
                        )
                    ) {
                        // OEM's legacy getter writes mode 4 while reading mode 3 on some devices.
                        // The native menu has a separate, explicit confirmation path for extreme.
                        param.result = SettingsRefreshRateSelectionRules.DEFAULT_HIGH_MODE
                    }
                }
            })
            ModernHookRegistry.installCompat(prefix + "persisted-choice", choice, object : ModernMethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!LspConfig.isSettingsRefreshRateUnlockedXposed()) return
                    val context = param.args.firstOrNull() as? Context ?: return
                    val stored = Settings.Secure.getInt(
                        context.contentResolver, SettingsRefreshRateSelectionRules.CHOICE_KEY, -1,
                    )
                    // The module's initialization happens in another process. The OEM cache is
                    // not invalidated by an external write and must not resurrect an old choice.
                    if (stored > 0) param.result = stored
                }
            })
            HookLog.i(TAG, "native default-high selection guards installed")
        } catch (error: Throwable) {
            ModernHookRegistry.unhookPrefix(prefix)
            throw error
        }
    }
}
