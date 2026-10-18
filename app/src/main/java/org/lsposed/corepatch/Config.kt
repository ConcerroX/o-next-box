// Adapted from LSPosed/CorePatch; settings are isolated and frozen until reboot.
package org.lsposed.corepatch

import com.mi.onextbox.lsp.CorePatchSettings

object Config {
    const val BYPASS_DOWNGRADE = "downgrade"
    const val BYPASS_MIN_SDK = "bypass_min_sdk"
    const val BYPASS_VERIFICATION = "bypass_verification"
    const val BYPASS_RESOURCE_ARSC_RESTRICTIONS = "bypass_resource_arsc_restrictions"
    const val BYPASS_DIGEST = "bypass_digest"
    const val BYPASS_EXACT_SIGNATURE_MATCH = "bypass_exact_sig_match"
    const val USE_PREVIOUS_SIGNATURES = "use_previous_signatures"
    const val ALLOW_HIDDEN_APIS_FOR_SYSTEM_APPS = "allow_hidden_apis_for_system_apps"
    const val BYPASS_SHARED_USER = "bypass_shared_user"
    const val DISABLE_VERIFICATION_AGENT = "disable_verification_agent"
    const val BYPASS_BLOCK = "bypass_block"

    private val allConfig = arrayOf(
        BYPASS_DOWNGRADE,
        BYPASS_MIN_SDK,
        BYPASS_VERIFICATION,
        BYPASS_RESOURCE_ARSC_RESTRICTIONS,
        BYPASS_DIGEST,
        USE_PREVIOUS_SIGNATURES,
        ALLOW_HIDDEN_APIS_FOR_SYSTEM_APPS,
        BYPASS_SHARED_USER,
        BYPASS_BLOCK
    )

    fun printAllConfig() {
        allConfig.forEach {
            XposedHelper.log("$it: ${CorePatchSettings.active(it)}")
        }
    }

    fun isBypassDowngradeEnabled(): Boolean {
        return CorePatchSettings.active(BYPASS_DOWNGRADE)
    }

    fun isBypassMinSdkEnabled(): Boolean {
        return CorePatchSettings.active(BYPASS_MIN_SDK)
    }

    fun isBypassVerificationEnabled(): Boolean {
        return CorePatchSettings.active(BYPASS_VERIFICATION)
    }

    fun isBypassResourceArscRestrictionsEnabled(): Boolean {
        return CorePatchSettings.active(BYPASS_RESOURCE_ARSC_RESTRICTIONS)
    }

    fun isBypassDigestEnabled(): Boolean {
        return CorePatchSettings.active(BYPASS_DIGEST)
    }

    fun isBypassExactSignatureMatch(): Boolean {
        return CorePatchSettings.active(BYPASS_EXACT_SIGNATURE_MATCH)
    }

    fun isUsePreviousSignaturesEnabled(): Boolean {
        return CorePatchSettings.active(USE_PREVIOUS_SIGNATURES)
    }

    fun isAllowHiddenApisForSystemAppsEnabled(): Boolean {
        return CorePatchSettings.active(ALLOW_HIDDEN_APIS_FOR_SYSTEM_APPS)
    }

    fun isBypassSharedUserEnabled(): Boolean {
        return CorePatchSettings.active(BYPASS_SHARED_USER)
    }

    fun isDisableVerificationAgentEnabled(): Boolean {
        return CorePatchSettings.active(DISABLE_VERIFICATION_AGENT)
    }

    fun isBypassBlockEnabled(): Boolean {
        return CorePatchSettings.active(BYPASS_BLOCK)
    }

}
