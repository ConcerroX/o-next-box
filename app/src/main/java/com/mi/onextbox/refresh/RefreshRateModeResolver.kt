package com.mi.onextbox.refresh

import kotlin.math.abs

internal object RefreshRateModeResolver {
    private val pattern = Regex("""id=(-?\d+), config=(-?\d+), (\d+)x(\d+)@([0-9.]+)""")

    fun resolve(saved: SavedRefreshRateMode, lines: List<String>): SavedRefreshRateMode? {
        val matches = lines.mapNotNull { line ->
            val match = pattern.find(line) ?: return@mapNotNull null
            val (id, config, width, height, rate) = match.destructured
            SavedRefreshRateMode(id.toIntOrNull() ?: return@mapNotNull null,
                config.toIntOrNull() ?: return@mapNotNull null,
                width.toIntOrNull() ?: return@mapNotNull null,
                height.toIntOrNull() ?: return@mapNotNull null,
                rate.toFloatOrNull() ?: return@mapNotNull null)
        }.filter { it.surfaceFlingerModeIndex >= 0 && it.width == saved.width && it.height == saved.height &&
            it.refreshRate.isFinite() && abs(it.refreshRate - saved.refreshRate) < 0.5f }
        return matches.firstOrNull { it.id == saved.id && it.surfaceFlingerModeIndex == saved.surfaceFlingerModeIndex }
            ?: matches.firstOrNull { it.id == saved.id }
            ?: matches.firstOrNull()
    }
}
