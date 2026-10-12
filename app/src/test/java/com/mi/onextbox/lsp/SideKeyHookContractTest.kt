package com.mi.onextbox.lsp

import com.mi.onextbox.R
import com.mi.onextbox.ui.screens.FeaturePageMode
import com.mi.onextbox.ui.screens.FeatureSearchIndex
import com.mi.onextbox.ui.screens.SideKeyFeatureItems
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SideKeyHookContractTest {
    @Test fun onlyDedicatedRawKeyIsInterceptedAndDisabledStateFallsThrough() {
        val source = source("lsp/SideKeyHooker.kt")
        assertTrue(source.contains("event.keyCode != SideKeyRules.RAW_KEY"))
        assertTrue(source.contains("if (!machine.isPressed && !SideKeySettings.enabledXposed()) return false"))
        assertTrue(source.contains("nativePressed.getBoolean(strategy)"))
        assertTrue(source.contains("checkConditions.invoke(strategy, true)"))
        assertFalse(source.contains("KEYCODE_POWER"))
        assertFalse(source.contains("KEYCODE_VOLUME"))
    }

    @Test fun timersAndReceiversHaveHotReloadOwnershipAndBoundedWakeLocks() {
        val hook = source("lsp/SideKeyHooker.kt")
        assertTrue(hook.contains("detach = owner::abort"))
        assertTrue(hook.contains("canReload = { owner.idle() }"))
        assertTrue(hook.contains("resumeAfterRejection = owner::resume"))
        assertTrue(hook.contains("handler.removeCallbacks(timer)"))
        assertTrue(hook.contains("wakeLock?.acquire(5000)"))
        assertTrue(hook.contains("pressExpiresAt = now + 5000"))
        val ui = source("lsp/SideKeySystemUiHooker.kt")
        assertTrue(ui.contains("capture = { owner.context }"))
        assertTrue(ui.contains("detach = owner::detach"))
        assertTrue(ui.contains("unregisterReceiver(receiver)"))
        assertTrue(ui.contains("unregisterTorchCallback(torch)"))
        assertTrue(ui.contains("HotReloadLifecycle.afterCommit { owner.attach"))
    }

    @Test fun appActionsUseNativeAuthenticationAndProtectedBroadcasts() {
        val source = source("lsp/SideKeySystemUiHooker.kt")
        assertEquals("android.permission.STATUS_BAR_SERVICE", SideKeyRules.PERMISSION)
        assertTrue(source.contains("SideKeyRules.PERMISSION, main, Context.RECEIVER_EXPORTED"))
        assertTrue(source.contains("postStartActivityDismissingKeyguard"))
        assertTrue(source.contains("activity.exported && activity.packageName == candidate.`package`"))
        assertTrue(source.contains("age !in 0..2000"))
        assertFalse(source.contains("disableKeyguard"))
        assertFalse(source.contains("dismissKeyguardThenExecute"))
        assertFalse(source.contains("setShowWhenLocked"))
    }

    @Test fun allFiveRequestedPresetsAreRetainedButNotSearchable() {
        val presets = setOf(SideKeyAction.WeChatScan, SideKeyAction.WeChatPay,
            SideKeyAction.AlipayScan, SideKeyAction.AlipayPay, SideKeyAction.AlipayTransit)
        SideKeyGesture.entries.forEach { gesture ->
            assertTrue(SideKeyFeatureItems.actionsFor(gesture).map { it.first }.containsAll(presets))
        }
        assertFalse(SideKeyFeatureItems.actionsFor(SideKeyGesture.Double).any { it.first == SideKeyAction.Stock })
        val titles = listOf(R.string.side_key_title, R.string.side_key_wechat_scan, R.string.side_key_wechat_pay,
            R.string.side_key_alipay_scan, R.string.side_key_alipay_pay, R.string.side_key_alipay_transit)
        titles.forEach { title ->
            assertFalse(FeatureSearchIndex.entries.any { it.title == title })
        }
        assertFalse(FeatureSearchIndex.entries.any { it.page == FeaturePageMode.SideKey })
    }

    @Test fun dispatcherAndResetContainTheNewFeature() {
        val entry = source("lsp/OosLspModuleEntry.kt")
        assertTrue(entry.contains("SideKeyHooker.hookSystemServer(loader)"))
        assertTrue(entry.contains("SideKeySystemUiHooker.hook(classLoader)"))
        assertTrue(source("lsp/LspConfig.kt").contains("SideKeySettings.save(context, false, SideKeyConfig())"))
        val settings = source("lsp/SideKeySettings.kt")
        assertTrue(settings.contains("values[ENABLED] as? Boolean ?: false"))
        assertTrue(settings.contains("LspConfig.syncReadableState(context)"))
    }

    @Test fun paymentPresetsContainNoReferralTracking() {
        val source = source("lsp/SideKeyPresetIntents.kt")
        listOf("10000007", "20000056", "200011235", "launch_type_offline_wallet").forEach {
            assertTrue(source.contains(it))
        }
        listOf("partnerId", "shareUserId", "chInfo").forEach { assertFalse(source.contains(it)) }
    }

    private fun source(path: String): String = listOf(File("src/main/java/com/mi/onextbox/$path"),
        File("app/src/main/java/com/mi/onextbox/$path")).first { it.isFile }.readText()
}
