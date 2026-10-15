package com.mi.onextbox.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.mi.onextbox.R
import com.mi.onextbox.ui.common.LogRecord
import com.mi.onextbox.ui.common.hookEvidenceRecords

@Composable
internal fun HookEvidenceSummary(records: List<LogRecord>) {
    val evidence = remember(records) { hookEvidenceRecords(records) }
    if (evidence.isEmpty()) return
    SettingsSection(stringResource(R.string.hook_evidence_title))
    SettingsGroup {
        SettingsCardRow(title = stringResource(R.string.hook_evidence_notice), summary = "",
            hasDividerBelow = true)
        evidence.forEachIndexed { index, item ->
            SettingsDivider()
            val feature = if (item.feature == "stacked_signal") R.string.feature_c16_stacked_signal_title else R.string.qs_tile_corner_title
            val state = when (item.state) {
                "observed" -> R.string.hook_evidence_observed
                "installed" -> R.string.hook_evidence_installed
                else -> R.string.hook_evidence_unavailable
            }
            SettingsCardRow(title = stringResource(feature),
                summary = "${stringResource(state)} · ${item.timestamp} · PID ${item.pid}",
                hasDividerAbove = true, hasDividerBelow = index < evidence.lastIndex)
        }
    }
}
