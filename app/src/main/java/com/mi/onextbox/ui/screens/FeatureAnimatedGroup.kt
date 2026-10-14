package com.mi.onextbox.ui.screens

import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import com.mi.onextbox.ui.common.AppUiStyle
import com.mi.onextbox.ui.common.LocalAppUiStyle
import com.mi.onextbox.ui.settings.Material3ExpressiveAnimatedSegmentPosition
import com.mi.onextbox.ui.settings.SettingsDivider
import com.mi.onextbox.ui.settings.SettingsGroup

internal class FeatureAnimatedGroupScope(private val retainHiddenRows: Boolean = false) {
    internal val rows = mutableListOf<Pair<Any, @Composable (Boolean, Boolean) -> Unit>>()
    internal val visibility = mutableMapOf<Any, Boolean>()

    fun row(key: Any, visible: Boolean = true, content: @Composable (above: Boolean, below: Boolean) -> Unit) {
        if (visible || retainHiddenRows) {
            rows += key to content
            visibility[key] = visible
        }
    }
}

/** One flat visible list: no empty animation slots or leftover MD3E inter-card gaps. */
@Composable
internal fun FeatureAnimatedGroup(animateChanges: Boolean = false, content: FeatureAnimatedGroupScope.() -> Unit) {
    SettingsGroup { FeatureAnimatedRows(animateChanges = animateChanges, content = content) }
}

@Composable
internal fun FeatureAnimatedRows(
    hasDividerAbove: Boolean = false,
    hasDividerBelow: Boolean = false,
    animateChanges: Boolean = false,
    content: FeatureAnimatedGroupScope.() -> Unit,
) {
    val scope = FeatureAnimatedGroupScope(retainHiddenRows = animateChanges).apply(content)
    if (animateChanges) {
        FeatureTransitionRows(scope, hasDividerAbove, hasDividerBelow)
        return
    }
    val rows = scope.rows
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

/** Opt-in real entry/exit and keyed placement animations without empty hidden slots. */
@Composable
private fun FeatureTransitionRows(scope: FeatureAnimatedGroupScope, hasDividerAbove: Boolean, hasDividerBelow: Boolean) {
    val material = LocalAppUiStyle.current == AppUiStyle.Material3Expressive
    val transitions = scope.rows.map { (identity, _) ->
        key(identity) {
            remember { MutableTransitionState(scope.visibility.getValue(identity)) }.apply {
                targetState = scope.visibility.getValue(identity)
            }
        }
    }
    // Exiting content stays composed through the entire animation. Finished hidden
    // rows have no placement, segment, divider or MD3E gap in the joined card.
    val rendered = scope.rows.indices.filter { index ->
        val transition = transitions[index]
        transition.currentState || transition.targetState || !transition.isIdle
    }
    LookaheadScope {
        val lookahead = this
        Column {
            rendered.forEachIndexed { position, rowIndex ->
                val (identity, row) = scope.rows[rowIndex]
                key(identity) {
                    val above = position > 0 || hasDividerAbove
                    val below = position < rendered.lastIndex || hasDividerBelow
                    // Keep visibility outside animateBounds: its outer height must
                    // be free to collapse, not fixed by bounds-animation constraints.
                    androidx.compose.animation.AnimatedVisibility(
                        visibleState = transitions[rowIndex],
                        enter = expandVertically(tween(280, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(180)),
                        exit = shrinkVertically(tween(280, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(180)),
                    ) {
                        Box(Modifier.animateBounds(lookahead, boundsTransform = { _, _ -> tween(280, easing = FastOutSlowInEasing) })) {
                            Column {
                                if (material) {
                                    Material3ExpressiveAnimatedSegmentPosition(
                                        position + if (hasDividerAbove) 1 else 0,
                                        rendered.size + (if (hasDividerAbove) 1 else 0) + (if (hasDividerBelow) 1 else 0),
                                        280,
                                    ) { row(above, below) }
                                    if (position < rendered.lastIndex) Spacer(Modifier.height(2.dp))
                                } else {
                                    row(above, below)
                                    if (below) SettingsDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
