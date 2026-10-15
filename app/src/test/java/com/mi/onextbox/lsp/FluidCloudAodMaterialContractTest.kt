package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Reflection contracts cannot be executed with the host JVM's Android stubs. */
class FluidCloudAodMaterialContractTest {
    @Test fun customAodCallbackPreservesNativeContentsThenSynchronizesMaterial() {
        val source = source("FluidCloudMaterialHooker.kt")
            .substringAfter("\"\$TAG:panel:aodProgress\"").substringBefore("changer.declaredMethods")
        assertTrue(source.indexOf("chain.proceed()") < source.indexOf("material.updatePanelAod(view, fraction)"))
        assertTrue(source.contains("chain.getArg(1)"))
        assertFalse(source.contains("changeAodViewColor"))
    }

    @Test fun shaderUsesNativeNotificationInterpolationAndRuntimeColors() {
        val source = source("FluidCloudAodMaterial.kt")
        assertTrue(source.contains("access\\\$readBgAodData"))
        assertTrue(source.contains("access\\\$createBgAodColor"))
        assertTrue(source.contains("\"writeBgAodData\""))
        assertTrue(source.contains("\"getColor\", fraction"))
        assertTrue(source.contains("getIdentifier(name, \"color\", \"com.android.systemui\")"))
        assertTrue(source.contains("aod_notification_card_\${name}_top_layer"))
        assertTrue(source.contains("aod_notification_card_\${name}_bottom_layer"))
        assertFalse(source.contains("Color.parseColor"))
        assertFalse(source.contains("0xff"))
    }

    @Test fun newMixIsAppliedBeforeAodInterpolationReadsIt() {
        val source = source("FluidCloudNotificationMaterial.kt")
            .substringAfter("fun preparePanel(").substringBefore("fun updatePanelAod(")
        assertTrue(source.indexOf("\"applyBlurConfig\"") < source.indexOf("aod.prepare("))
        assertTrue(source.contains("if (fraction == 0f) native.ensureNotificationMetaBall"))
        assertTrue(source.contains("native.applyAod(config, key.mode, fraction)"))
    }

    @Test fun nativeInverseClearMaskHasIsolatedLayerAndLastDrawOrder() {
        val source = source("FluidCloudAodMaterial.kt")
        assertTrue(source.contains("canvas.saveLayer(RectF(mask.bounds), null)"))
        assertTrue(source.contains("finally { canvas.restoreToCount(save) }"))
        val hook = source("FluidCloudMaterialHooker.kt")
        assertTrue(hook.indexOf("material.drawPanelColor(") < hook.indexOf("material.drawPanelAod("))
        assertTrue(hook.contains("aodScope.hidesMask(chain.thisObject)"))
        assertTrue(hook.contains("aodScope.duringDraw("))
    }

    @Test fun exitDisableAndReloadRestoreOwnedState() {
        val source = source("FluidCloudAodMaterial.kt")
        assertTrue(source.contains("\"setAodState\", false"))
        assertTrue(source.contains("\"changeBlurColor\", origin, true"))
        assertTrue(source.contains("\"panoramicAodDrawable\") === state.mask"))
        assertTrue(source.indexOf("states[view] = state") < source.indexOf("\"setAodState\", true"))
        val material = source("FluidCloudNotificationMaterial.kt")
        assertTrue(material.contains("aod.capture()"))
        assertTrue(material.contains("aod.releaseAll()"))
        assertTrue(material.contains("panels.keys.toList().forEach(::releasePanelAod)"))
        assertTrue(material.contains("api().applyAod(config, mode, 0f)"))
        assertTrue(material.contains("HotReloadLifecycle.isRetiring || !LspConfig.isFluidCloudMaterialEnabledXposed()"))
    }

    @Test fun noGlobalDozePolicyOrRealNotificationTypeIsRewritten() {
        for (file in listOf("FluidCloudAodMaterial.kt", "FluidCloudMaterialHooker.kt", "FluidCloudNotificationMaterial.kt")) {
            // Expanded views own a separate proxy; this contract concerns real native rows.
            val source = source(file).substringBefore("private inner class MaterialDrawable")
            for (method in listOf("\"setCardType\"", "\"setDozing\"", "\"setPanoramicAodProgress\"", "\"setForeground\"")) {
                assertFalse("$file must not call $method", source.contains(method))
            }
        }
    }

    private fun source(name: String): String = listOf(
        File("src/main/java/com/mi/onextbox/lsp/$name"),
        File("app/src/main/java/com/mi/onextbox/lsp/$name"),
    ).first { it.isFile }.readText()
}
