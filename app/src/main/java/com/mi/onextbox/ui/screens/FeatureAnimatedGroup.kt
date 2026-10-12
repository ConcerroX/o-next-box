package com.mi.onextbox.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mi.onextbox.ui.common.AppUiStyle
import com.mi.onextbox.ui.common.LocalAppUiStyle
import com.mi.onextbox.ui.settings.Material3ExpressiveAnimatedSegmentPosition
import com.mi.onextbox.ui.settings.SettingsDivider
import com.mi.onextbox.ui.settings.SettingsGroup

internal class FeatureAnimatedGroupScope {
    internal val rows = mutableListOf<Pair<Any, @Composable (Boolean, Boolean) -> Unit>>()

    fun row(key: Any, visible: Boolean = true, content: @Composable (above: Boolean, below: Boolean) -> Unit) {
        if (visible) rows += key to content
    }
}

/** One flat visible list: no empty animation slots or leftover MD3E inter-card gaps. */
@Composable
internal fun FeatureAnimatedGroup(content: FeatureAnimatedGroupScope.() -> Unit) {
    SettingsGroup { FeatureAnimatedRows(content = content) }
}

@Composable
internal fun FeatureAnimatedRows(
    hasDividerAbove: Boolean = false,
    hasDividerBelow: Boolean = false,
    content: FeatureAnimatedGroupScope.() -> Unit,
) {
    val rows = FeatureAnimatedGroupScope().apply(content).rows
    val material = LocalAppUiStyle.current == AppUiStyle.Material3Expressive
    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(if (material) 2.dp else 0.dp),
    ) {
        rows.forEachIndexed { index, (identity, row) ->
            key(identity) {
                val above = index > 0 || hasDividerAbove
                val below = index < rows.lastIndex || hasDividerBelow
                if (material) {
                    Material3ExpressiveAnimatedSegmentPosition(
                        index + if (hasDividerAbove) 1 else 0,
                        rows.size + (if (hasDividerAbove) 1 else 0) + (if (hasDividerBelow) 1 else 0),
                        280,
                    ) { row(above, below) }
                } else {
                    row(above, below)
                    if (below) SettingsDivider()
                }
            }
        }
    }
}
