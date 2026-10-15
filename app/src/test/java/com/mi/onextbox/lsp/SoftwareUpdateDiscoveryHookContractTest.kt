package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SoftwareUpdateDiscoveryHookContractTest {
    @Test fun otaReturnsNativeNoUpdateCompletionAndRejectsInFlightResponses() {
        val hook = source("SoftwareUpdateHooker.kt")
        assertTrue(hook.contains("blocksOtaMessage(message.what, enabled(SoftwareUpdateFeature.BlockSystemUpdate))"))
        assertTrue(hook.contains("sendEmptyMessage(SoftwareUpdateDiscoveryRules.NO_OTA_UPDATE)"))
        assertTrue(hook.contains("SoftwareUpdateDiscoveryRules.NO_VERSION_RESULT else chain.proceed()"))
        assertTrue(hook.contains("install(\"versionQuery\", target)"))
        assertTrue(hook.contains("Modifier.isFinal(it.modifiers)"))
        assertTrue(hook.contains("callback.sendEmptyMessage(SoftwareUpdateDiscoveryRules.NO_OTA_UPDATE)"))
    }

    @Test fun mergedOpexAndStandaloneOpexAreGuardedByTheSameIndependentSwitch() {
        val hook = source("SoftwareUpdateHooker.kt")
        val standalone = source("SystemFileUpdateHooker.kt")
        assertTrue(hook.contains("disableMergedOpex(report"))
        assertTrue(hook.contains("install(\"mergedOpex\", target)"))
        assertTrue(hook.contains("\"IS_SAME_LIST\""))
        assertTrue(standalone.contains("blocksOpexTask(task, enabled())"))
        assertTrue(standalone.contains("SoftwareUpdateFeature.BlockSystemFiles"))
        assertFalse(standalone.contains("SoftwareUpdateFeature.BlockSystemUpdate"))
    }

    @Test fun providerAuthorizationAndActiveProgressAreHandledByStockCodeFirst() {
        val hook = source("SystemFileUpdateHooker.kt")
        assertTrue(hook.contains("val result = chain.proceed() as? Bundle ?: return@Hooker null"))
        assertTrue(hook.contains("result.containsKey(\"opexStatus\")"))
        assertTrue(hook.contains("Bundle(result).apply"))
        assertFalse(hook.contains("getCallingPackage"))
        assertFalse(hook.contains("queryOpexUpdate\""))
    }

    @Test fun serviceAndCacheRemainIntactAndHooksHaveGenerationOwnership() {
        val hook = source("SystemFileUpdateHooker.kt")
        for (forbidden in listOf("stopSelf", "cancel", "setComponentEnabledSetting", "delete(", "Settings.Secure.put", "SAUAppService")) {
            assertFalse(forbidden, hook.contains(forbidden))
        }
        assertTrue(hook.contains("HotReloadLifecycle.register(\"\$prefix:targets\", capture = { targets })"))
        assertTrue(hook.contains("ModernHookRegistry.installFast"))
        assertTrue(hook.contains("deoptimize(it)"))
        assertTrue(source("SoftwareUpdateTargets.kt").contains("schema:update-discovery-v4"))
    }

    private fun source(path: String) = listOf(File("src/main/java/com/mi/onextbox/lsp/$path"),
        File("app/src/main/java/com/mi/onextbox/lsp/$path")).first { it.isFile }.readText()
}
