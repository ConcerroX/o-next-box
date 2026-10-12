package com.mi.onextbox.lsp

import android.content.Intent
import android.net.Uri

internal object SideKeyPresetIntents {
    const val WECHAT = "com.tencent.mm"
    const val ALIPAY = "com.eg.android.AlipayGphone"

    fun candidates(action: SideKeyAction): List<Intent> = when (action) {
        SideKeyAction.WeChatScan -> listOf(
            Intent("com.tencent.mm.action.BIZSHORTCUT").setPackage(WECHAT)
                .putExtra("LauncherUI.From.Scaner.Shortcut", true),
            Intent("com.tencent.mm.ui.ShortCutDispatchAction").setPackage(WECHAT)
                .putExtra("LauncherUI.Shortcut.LaunchType", "launch_type_scan"),
        )
        SideKeyAction.WeChatPay -> listOf(
            Intent("com.tencent.mm.ui.ShortCutDispatchAction").setPackage(WECHAT)
                .putExtra("LauncherUI.Shortcut.LaunchType", "launch_type_offline_wallet"),
            Intent("com.nearme.pay.qrcode").setPackage(WECHAT)
                .putExtra("extra_channel", "wxpay").putExtra("extra_pkg_name", "com.android.systemui"),
        )
        SideKeyAction.AlipayScan -> listOf(alipay("10000007"))
        SideKeyAction.AlipayPay -> listOf(alipay("20000056"))
        SideKeyAction.AlipayTransit -> listOf(alipay("200011235"))
        else -> emptyList()
    }.map { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) }

    private fun alipay(id: String) = Intent(Intent.ACTION_VIEW,
        Uri.parse("alipays://platformapi/startapp?appId=$id")).setPackage(ALIPAY)
}
