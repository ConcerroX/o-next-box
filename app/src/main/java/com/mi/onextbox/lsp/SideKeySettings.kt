package com.mi.onextbox.lsp

import android.content.Context
import android.content.SharedPreferences
import com.mi.onextbox.lsp.compat.HookConfigSnapshot
import kotlinx.coroutines.Deferred

internal object SideKeySettings {
    const val ENABLED = "side_key_enabled"
    const val LONG_MILLIS = "side_key_long_millis"
    const val DOUBLE_MILLIS = "side_key_double_millis"
    fun actionKey(gesture: SideKeyGesture) = "side_key_${gesture.id}_action"
    fun packageKey(gesture: SideKeyGesture) = "side_key_${gesture.id}_package"
    private fun default(gesture: SideKeyGesture) =
        if (gesture == SideKeyGesture.Double) SideKeyAction.None else SideKeyAction.Stock

    @Volatile
    private var updateQueue: SideKeyUpdateQueue? = null

    private fun queue(context: Context): SideKeyUpdateQueue = updateQueue ?: synchronized(this) {
        updateQueue ?: context.applicationContext.let { application ->
            SideKeyUpdateQueue(
                read = { state(application) },
                write = { persist(application, it) },
            ).also { updateQueue = it }
        }
    }

    fun enabled(context: Context) = state(context).enabled
    fun enabledXposed() = SideKeyFeaturePolicy.available && HookConfigSnapshot.boolean(ENABLED, false) == true
    fun config(context: Context): SideKeyConfig = state(context).config

    fun state(context: Context): SideKeyState {
        val values = LspPreferenceStore.prefs(context).all
        fun binding(gesture: SideKeyGesture) = SideKeyBinding(
            SideKeyAction.parse(values[actionKey(gesture)] as? String, default(gesture)),
            (values[packageKey(gesture)] as? String).orEmpty(),
        )
        val config = SideKeyConfig(binding(SideKeyGesture.Single), binding(SideKeyGesture.Double),
            binding(SideKeyGesture.Long), SideKeyRules.longInterval(values[LONG_MILLIS] as? Int ?: 495).toLong(),
            SideKeyRules.doubleInterval(values[DOUBLE_MILLIS] as? Int ?: 300).toLong())
        return SideKeyFeaturePolicy.normalize(SideKeyState(values[ENABLED] as? Boolean ?: false, config))
    }

    fun configXposed(): SideKeyConfig {
        fun binding(gesture: SideKeyGesture) = SideKeyBinding(
            SideKeyAction.parse(HookConfigSnapshot.string(actionKey(gesture), null), default(gesture)),
            HookConfigSnapshot.string(packageKey(gesture), "").orEmpty(),
        )
        return SideKeyConfig(binding(SideKeyGesture.Single), binding(SideKeyGesture.Double),
            binding(SideKeyGesture.Long), SideKeyRules.longInterval(HookConfigSnapshot.int(LONG_MILLIS, 495) ?: 495).toLong(),
            SideKeyRules.doubleInterval(HookConfigSnapshot.int(DOUBLE_MILLIS, 300) ?: 300).toLong())
    }

    fun save(context: Context, enabled: Boolean, config: SideKeyConfig) {
        queue(context).replace(SideKeyFeaturePolicy.normalize(SideKeyState(enabled, config)))
    }

    fun enqueueUpdate(context: Context, transform: (SideKeyState) -> SideKeyState): Deferred<SideKeyState> =
        queue(context).enqueue { SideKeyFeaturePolicy.normalize(transform(it)) }

    fun enforceDisabled(preferences: SharedPreferences) {
        if (!SideKeyFeaturePolicy.available && preferences.all[ENABLED] != false) {
            check(preferences.edit().putBoolean(ENABLED, false).commit())
        }
    }

    private fun persist(context: Context, nextState: SideKeyState): SideKeyState {
        val config = nextState.config
        val edit = LspPreferenceStore.prefs(context).edit().putBoolean(ENABLED, SideKeyFeaturePolicy.enabled(nextState.enabled))
            .putInt(LONG_MILLIS, SideKeyRules.longInterval(config.longMillis.toInt()))
            .putInt(DOUBLE_MILLIS, SideKeyRules.doubleInterval(config.doubleMillis.toInt()))
        SideKeyGesture.entries.forEach { gesture ->
            val binding = config.binding(gesture)
            val action = if (gesture == SideKeyGesture.Double && binding.action == SideKeyAction.Stock)
                SideKeyAction.None else binding.action
            edit.putString(actionKey(gesture), action.id)
                .putString(packageKey(gesture), binding.packageName.takeIf(SideKeyRules::validPackage).orEmpty())
        }
        check(edit.commit())
        LspConfig.syncReadableState(context)
        return state(context)
    }
}
