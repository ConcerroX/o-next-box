package com.mi.onextbox.lsp

/** Unlocking exposes choices; only an explicit native menu selection should enable extreme mode. */
internal object SettingsRefreshRateSelectionRules {
    const val MODE_KEY = "oplus_customize_screen_refresh_rate"
    const val CHOICE_KEY = "choice_mode_rate"
    const val DEFAULT_HIGH_MODE = 3
    const val DEFAULT_HIGH_HZ = 120

    data class Initialization(val choiceHertz: Int, val mode: Int?)

    fun initializeOnEnable(
        wasEnabled: Boolean,
        enabled: Boolean,
        currentMode: Int,
        currentChoice: Int,
    ): Initialization? {
        if (wasEnabled || !enabled) return null
        // Preserve auto, standard and an active native 90 Hz mode, not stale extreme choices.
        val choiceHertz = when (currentMode) {
            1 -> 90
            0, 2, 3, 4, 7, 8 -> DEFAULT_HIGH_HZ
            else -> return null // Do not modify an unknown firmware mode or missing setting.
        }
        val mode = when (currentMode) {
            4, 7, 8 -> DEFAULT_HIGH_MODE
            else -> null
        }
        return if (currentChoice == choiceHertz && mode == null) null
        else Initialization(choiceHertz, mode)
    }

    fun keepDefaultHighMode(unlocked: Boolean, requestedMode: Int): Boolean =
        unlocked && requestedMode == DEFAULT_HIGH_MODE
}
