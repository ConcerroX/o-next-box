package com.mi.onextbox.lsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurePayReminderRulesTest {
    @Test fun disabledFeatureRetainsEvenTheNativeRiskChannel() {
        assertFalse(SecurePayReminderRules.shouldStripLiveAlert(false, SecurePayReminderRules.LIVE_CHANNEL))
    }

    @Test fun onlyTheExactRiskLiveNotificationChannelIsAffected() {
        assertTrue(SecurePayReminderRules.shouldStripLiveAlert(true, "secure_pay_live_update"))
        listOf(null, "", "secure_pay", "secure_pay_live_update_other", "other_live_update")
            .forEach { assertFalse(SecurePayReminderRules.shouldStripLiveAlert(true, it)) }
    }

    @Test fun promotionRemovalPreservesAllOtherNotificationFlags() {
        val promoted = 1 shl 18
        val unrelated = 2 or 16 or 64 or (1 shl 20)
        assertEquals(unrelated, SecurePayReminderRules.withoutPromotion(unrelated or promoted, promoted))
        assertEquals(unrelated, SecurePayReminderRules.withoutPromotion(unrelated, promoted))
        assertEquals(unrelated, SecurePayReminderRules.withoutPromotion(unrelated, 0))
    }

    @Test fun promotionRemovalIsIdempotentAcrossNotifyOverloads() {
        val promoted = 1 shl 18
        val first = SecurePayReminderRules.withoutPromotion(-1, promoted)
        assertEquals(first, SecurePayReminderRules.withoutPromotion(first, promoted))
    }

    @Test fun onlyVendorCloudMetadataIsRemovedNotNotificationTextOrActions() {
        assertEquals(setOf("oplus.livealert.card", "oplus.livealert.capsule", "oplusLiveAlertAppConfig"),
            SecurePayReminderRules.cloudExtras.toSet())
        assertFalse("android.text" in SecurePayReminderRules.cloudExtras)
        assertFalse("android.title" in SecurePayReminderRules.cloudExtras)
    }
}
