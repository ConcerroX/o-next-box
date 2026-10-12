package com.mi.onextbox.lsp

import android.content.Context
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

/** Reconnect the OEM's 90 Hz mode to its existing COUI menu, not a custom preference/listener. */
internal object SettingsNativeRefreshRateMenu {
    private const val TAG = "SettingsRefreshRate"
    private const val FRAGMENT = "com.oplus.settings.feature.display.ScreenRefreshRateFragment"
    private val reportedFailure = AtomicBoolean(false)

    fun hook(loader: ClassLoader, utils: Class<*>) {
        val fragment = ModernReflect.findClass(FRAGMENT, loader)
        val preferenceClass = ModernReflect.findClass("androidx.preference.Preference", loader)
        val menuClass = ModernReflect.findClass(
            "com.oplus.settings.feature.display.RefreshRateMenuPreference", loader,
        )
        val intType = Int::class.javaPrimitiveType!!
        fun method(type: Class<*>, name: String, result: Class<*>, vararg args: Class<*>): Method =
            ModernReflect.findMethodExact(type, name, args).also { check(it.returnType == result) }

        // Resolve the complete contract first: partial menu interception could silently apply 120 Hz.
        val setup = method(fragment, "setupRefreshRateSettingEntries", Void.TYPE, intType)
        val selection = method(fragment, "lambda\$setupRefreshRateSettingEntries\$1",
            Boolean::class.javaPrimitiveType!!, intType, preferenceClass, Any::class.java)
        val summary = method(fragment, "refreshRateSettingSummary", Void.TYPE, intType, intType)
        val apply = method(fragment, "applyRefreshRateSetting", Void.TYPE, intType)
        val classify = method(utils, "isHighRefreshModeIndex", Boolean::class.javaPrimitiveType!!, intType)
        val normalize = method(utils, "normalizeHighRefreshGear", intType, Context::class.java, intType)
        val hzToIndex = method(utils, "hzToRateIndex", intType, intType)
        val indexToHz = method(utils, "rateIndexToHz", intType, intType)
        val choice = method(utils, "getChoiceModeRate", intType, Context::class.java)
        val bidiWrap = method(fragment, "bidiWrap", String::class.java, Context::class.java, String::class.java)
        val formatHz = method(fragment, "formatRateHz", String::class.java, intType)
        val chars = arrayOf<CharSequence>().javaClass
        val getEntries = method(menuClass, "getEntries", chars)
        val getValues = method(menuClass, "getEntryValues", chars)
        val setEntries = method(menuClass, "setEntries", Void.TYPE, chars)
        val setValues = method(menuClass, "setEntryValues", Void.TYPE, chars)
        val setValue = method(menuClass, "setValue", Void.TYPE, String::class.java)
        val setAssignment = method(menuClass, "setAssignment", Void.TYPE, CharSequence::class.java)
        val contextField = fragment.getDeclaredField("mContext").apply { isAccessible = true }
        val menuField = fragment.getDeclaredField("mRefreshRateSettingPreference").apply { isAccessible = true }
        val currentRates = utils.getDeclaredField("sCurrentDisplayRefreshRateList").apply { isAccessible = true }
        val groups = ModernReflect.findClass("com.coui.appcompat.preference.COUIMenuPreference", loader)
            .getDeclaredField("mGroupIds").apply { isAccessible = true }

        fun native90Mapped(): Boolean =
            hzToIndex.invoke(null, SettingsRefreshRateMenuRules.RATE_90) == SettingsRefreshRateMenuRules.NATIVE_90_INDEX &&
                indexToHz.invoke(null, SettingsRefreshRateMenuRules.NATIVE_90_INDEX) == SettingsRefreshRateMenuRules.RATE_90

        fun enabled(context: Context): Boolean {
            if (!LspConfig.isSettingsRefreshRateUnlockedXposed() || !native90Mapped()) return false
            val display = context.display
            fun android.view.Display.Mode.spec() = SettingsRefreshRateMenuRules.Mode(
                physicalWidth, physicalHeight, refreshRate,
            )
            return SettingsRefreshRateMenuRules.supports90(display.mode.spec(), display.supportedModes.map { it.spec() })
        }

        fun nativeLabel(context: Context): String? {
            val id = context.resources.getIdentifier("refresh_rate_hz_format", "string", "com.android.settings")
            if (id == 0) return null
            val label = context.getString(id, formatHz.invoke(null, SettingsRefreshRateMenuRules.RATE_90))
            return bidiWrap.invoke(null, context, label) as? String
        }

        fun safely(block: () -> Unit) {
            runCatching(block).onFailure {
                if (reportedFailure.compareAndSet(false, true)) HookLog.w(TAG, "native 90 Hz menu update failed", it)
            }
        }

        val prefix = "settings-native-90:${System.identityHashCode(loader)}:"
        try {
            ModernHookRegistry.installCompat(prefix + "entries", setup, object : ModernMethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.throwable != null) return
                    safely {
                        val owner = param.thisObject ?: return@safely
                        val context = contextField.get(owner) as? Context ?: return@safely
                        if (!enabled(context)) return@safely
                        val menu = menuField.get(owner) ?: return@safely
                        // A different firmware's grouped menu requires its own matching group array.
                        if (groups.get(menu) != null) return@safely
                        val labels = getEntries.invoke(menu) as? Array<*> ?: return@safely
                        val values = getValues.invoke(menu) as? Array<*> ?: return@safely
                        if (labels.any { it !is String } || values.any { it !is String }) return@safely
                        val merged = SettingsRefreshRateMenuRules.expose90(
                            labels.filterIsInstance<CharSequence>(), values.filterIsInstance<CharSequence>(),
                            nativeLabel(context) ?: return@safely,
                        ) ?: return@safely
                        setEntries.invoke(menu, merged.labels.toTypedArray() as Any)
                        setValues.invoke(menu, merged.values.toTypedArray() as Any)
                    }
                }
            })
            ModernHookRegistry.installCompat(prefix + "selection", selection, object : ModernMethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args.getOrNull(2) != SettingsRefreshRateMenuRules.VALUE_90) return
                    // A stale popup after disabling the feature must not fall through to OEM's 120 default.
                    param.result = false
                    safely {
                        val owner = param.thisObject ?: return@safely
                        val context = contextField.get(owner) as? Context ?: return@safely
                        if (!enabled(context)) return@safely
                        apply.invoke(owner, SettingsRefreshRateMenuRules.RATE_90)
                        param.result = true
                        HookLog.i(TAG, "native 90 Hz selection applied")
                    }
                }
            })
            ModernHookRegistry.installCompat(prefix + "classification", classify, object : ModernMethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.throwable != null || param.result != false ||
                        param.args.firstOrNull() != SettingsRefreshRateMenuRules.NATIVE_90_INDEX ||
                        !LspConfig.isSettingsRefreshRateUnlockedXposed()
                    ) return
                    safely {
                        // This context-free OEM method uses the list reloaded for the current display.
                        val rates = currentRates.get(null) as? List<*> ?: return@safely
                        if (native90Mapped() && rates.contains(SettingsRefreshRateMenuRules.NATIVE_90_INDEX)) {
                            param.result = true
                        }
                    }
                }
            })
            ModernHookRegistry.installCompat(prefix + "summary", summary, object : ModernMethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.throwable != null || param.args.getOrNull(1) != SettingsRefreshRateMenuRules.RATE_90) return
                    safely {
                        val owner = param.thisObject ?: return@safely
                        val context = contextField.get(owner) as? Context ?: return@safely
                        if (!enabled(context)) return@safely
                        val menu = menuField.get(owner) ?: return@safely
                        val values = getValues.invoke(menu) as? Array<*> ?: return@safely
                        if (values.none { it == SettingsRefreshRateMenuRules.VALUE_90 }) return@safely
                        setAssignment.invoke(menu, nativeLabel(context) ?: return@safely)
                        setValue.invoke(menu, SettingsRefreshRateMenuRules.VALUE_90)
                    }
                }
            })
            ModernHookRegistry.installCompat(prefix + "return-to-high", normalize, object : ModernMethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.throwable != null || (param.args.getOrNull(1) as? Int ?: 0) < 3) return
                    safely {
                        val context = param.args.firstOrNull() as? Context ?: return@safely
                        if (enabled(context) && choice.invoke(null, context) == SettingsRefreshRateMenuRules.RATE_90) {
                            param.result = SettingsRefreshRateMenuRules.NATIVE_90_INDEX
                        }
                    }
                }
            })
            HookLog.i(TAG, "native 90 Hz menu hooks installed")
        } catch (error: Throwable) {
            ModernHookRegistry.unhookPrefix(prefix)
            throw error
        }
    }
}
