package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CorePatchIntegrationTest {
    @Test fun emptyConfigurationCannotActivateAnyBypass() {
        for (key in listOf("downgrade", "bypass_verification", "bypass_shared_user", "use_previous_signatures"))
            assertFalse(CorePatchRules.active(key, emptySet()))
    }
    @Test fun dangerousOptionsRequireTheirDependencies() {
        assertFalse(CorePatchRules.active("bypass_shared_user", setOf("bypass_shared_user")))
        assertTrue(CorePatchRules.active("bypass_shared_user", setOf("bypass_shared_user", "bypass_digest")))
        assertFalse(CorePatchRules.active("use_previous_signatures", setOf("use_previous_signatures", "bypass_digest")))
        assertTrue(CorePatchRules.active("use_previous_signatures", setOf("use_previous_signatures", "bypass_digest", "bypass_verification")))
    }
    @Test fun hooksRemainScopedAndBootOnly() {
        val integration = File("src/main/java/com/mi/onextbox/lsp/CorePatchIntegration.kt").readText()
        assertTrue(integration.contains("if (!CorePatchSettings.captureBootOptions()) return"))
        assertTrue(integration.contains("canReload = { false }"))
        val helper = File("src/main/java/org/lsposed/corepatch/XposedHelper.kt").readText()
        assertTrue(helper.contains("ModernHookRegistry.installCompat"))
        assertFalse(helper.contains("getRemotePreferences"))
        val digest = File("src/main/java/org/lsposed/corepatch/hook/MessageDigestHook.kt").readText()
        assertTrue(digest.contains("it.className.startsWith(\"android.util.apk.\")"))
        val signing = File("src/main/java/org/lsposed/corepatch/hook/SigningDetailsHook.kt").readText()
        assertTrue(signing.contains("and (4 or 16) == 0"))
    }
}
