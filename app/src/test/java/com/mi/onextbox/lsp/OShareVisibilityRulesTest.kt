package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class OShareVisibilityRulesTest {
    @Test fun enabledOnlySuppressesEveryoneVisibilityTimeout() {
        assertTrue(OShareVisibilityRules.suppress(true, "oplus.action.oshare.switch.timeout"))
        listOf(null, "", "oplus.action.oshare.scan.timeout", "coloros.intent.action.OSHARE_STATE",
            "oshare_send_timeout", "oplus.action.oshare.switch.timeout.extra").forEach {
            assertFalse(OShareVisibilityRules.suppress(true, it))
        }
    }

    @Test fun disabledPreservesStockTimeout() {
        assertFalse(OShareVisibilityRules.suppress(false, OShareVisibilityRules.TIMEOUT_ACTION))
    }

    @Test fun enabledClearsUiCountdownWithoutRunningAutomaticRangeChange() {
        var cleared = false
        assertNull(OShareVisibilityRules.runCountdown(true, { cleared = true },
            { error("Must not execute the expiry branch") }, { throw AssertionError(it) }))
        assertTrue(cleared)
    }

    @Test fun disabledAndIncompatibleCountdownsRetainOriginalBehavior() {
        assertEquals("stock", OShareVisibilityRules.runCountdown(false,
            { error("Must not clear stock countdown") }, { "stock" }, {}))
        var failure = false
        assertEquals("stock", OShareVisibilityRules.runCountdown(true,
            { throw IllegalStateException() }, { "stock" }, { failure = true }))
        assertTrue(failure)
    }

    @Test fun visibilityLabelDropsOnlyTrailingDurationParentheses() {
        assertEquals("所有人", OShareVisibilityRules.unlimitedLabel("所有人（10 分钟）"))
        assertEquals("All", OShareVisibilityRules.unlimitedLabel("All (for 10 minutes)"))
        assertEquals("الكل", OShareVisibilityRules.unlimitedLabel("الكل (لمدة ١٠ دقائق)"))
        assertEquals("Everyone", OShareVisibilityRules.unlimitedLabel("Everyone"))
        assertEquals("(10 minutes)", OShareVisibilityRules.unlimitedLabel("(10 minutes)"))
    }

    @Test fun oshareIsAHotReloadSupportedScope() {
        assertTrue(ModuleHotReloadPolicy.supportsProcess("com.coloros.oshare", false))
        assertTrue(ModuleHotReloadPolicy.supportsProcess("com.coloros.oshare:transfer", false))
    }
}
