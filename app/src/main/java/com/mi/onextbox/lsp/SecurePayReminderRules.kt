package com.mi.onextbox.lsp

/** Presentation-only identifiers verified in SecurePay 17.5.0. */
internal object SecurePayReminderRules {
    const val CLOUD_OWNER = "com.oplus.securepay.scan.ui.ScanUIFluidCloud"
    const val RISK_CARD_LOG = "[showRiskCard]"
    const val LIVE_LOG_TAG = "ScanUILiveUpdateNotification"
    const val LIVE_REFRESH_LOG = "[refreshNotification] notification refreshed"
    const val LIVE_CHANNEL = "secure_pay_live_update"
    const val PROMOTION_EXTRA = "android.requestPromotedOngoing"
    val cloudExtras = listOf(
        "oplus.livealert.card",
        "oplus.livealert.capsule",
        "oplusLiveAlertAppConfig",
    )

    fun shouldStripLiveAlert(enabled: Boolean, channel: String?): Boolean =
        enabled && channel == LIVE_CHANNEL

    fun withoutPromotion(flags: Int, promotedFlag: Int): Int = flags and promotedFlag.inv()
}
