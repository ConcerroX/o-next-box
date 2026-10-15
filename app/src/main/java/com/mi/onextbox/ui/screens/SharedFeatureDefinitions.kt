package com.mi.onextbox.ui.screens

import android.content.Context
import com.mi.onextbox.R
import com.mi.onextbox.lsp.C16StackedSignalSettings
import com.mi.onextbox.lsp.NetworkIdentitySettings
import com.mi.onextbox.lsp.QsTileCornerSettings

/** Incremental migration: one definition drives search, inventory and quick switches. */
internal object SharedFeatureDefinitions {
    data class Toggle(
        val title: Int,
        val summary: Int,
        val page: FeaturePageMode,
        val read: suspend (Context) -> Boolean,
        val write: suspend (Context, Boolean) -> Unit,
    ) {
        fun catalogEntry() = EnabledFeatureCatalog.Entry(title, page, read = read)
        fun searchEntry() = FeatureSearchIndex.Entry(page, title, summary)
        fun action() = title to write
    }

    val networkIdentity = Toggle(R.string.network_identity_title, R.string.network_identity_summary,
        FeaturePageMode.MobileNetwork, { NetworkIdentitySettings.state(it).enabled },
        { context, enabled -> NetworkIdentitySettings.setEnabled(context, enabled) })
    val qsCorner = Toggle(R.string.qs_tile_corner_title, R.string.qs_tile_corner_summary,
        FeaturePageMode.SystemUiControlCenter, { QsTileCornerSettings.state(it).enabled },
        { context, enabled -> QsTileCornerSettings.setEnabled(context, enabled) })
    val stackedSignal = Toggle(R.string.feature_c16_stacked_signal_title, R.string.feature_c16_stacked_signal_summary,
        FeaturePageMode.SystemUiNative, { C16StackedSignalSettings.enabled(it) },
        { context, enabled -> C16StackedSignalSettings.set(context, enabled) })
    val all = listOf(networkIdentity, qsCorner, stackedSignal)
}
