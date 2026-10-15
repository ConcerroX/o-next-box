package com.mi.onextbox.lsp

internal object LauncherBlurRules {
    enum class Effect { Animation, Material }

    fun animationEnabled(setting: Int): Boolean = setting == -1 || setting == 1

    data class State(
        val animation: Boolean,
        val material: Boolean,
        val lowPower: Boolean,
        val superPowerSave: Boolean,
        val windowBlursDisabled: Boolean,
    ) {
        fun allows(effect: Effect): Boolean = !lowPower && !superPowerSave && !windowBlursDisabled &&
            when (effect) {
                Effect.Animation -> animation
                Effect.Material -> material
            }
    }
}

/** Only the native blur builder may bypass COUI's shared animation-tier test. */
internal class LauncherBlurScope {
    private val active = ThreadLocal<Boolean>()
    fun isActive(): Boolean = active.get() == true

    fun <T> within(block: () -> T): T {
        val previous = active.get()
        active.set(true)
        return try { block() } finally {
            if (previous == null) active.remove() else active.set(previous)
        }
    }
}
