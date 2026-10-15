package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class FluidCloudAodMaterialRulesTest {
    private val rules = FluidCloudAodMaterialRules

    @Test fun progressClampsNativeAnimationWithoutAcceptingInvalidValues() {
        assertEquals(0f, rules.progress(null), 0f)
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1f)) {
            assertEquals(0f, rules.progress(value), 0f)
        }
        assertEquals(0.4f, rules.progress(0.4f), 0f)
        assertEquals(1f, rules.progress(2f), 0f)
    }

    @Test fun fallbackMaskUsesNativeTruncatedAnimationAlpha() {
        assertEquals(0, rules.alpha(0f))
        assertEquals(127, rules.alpha(0.5f))
        assertEquals(255, rules.alpha(1f))
        assertEquals(0, rules.alpha(-1f))
        assertEquals(255, rules.alpha(2f))
        assertEquals(0, rules.alpha(Float.NaN))
    }

    @Test fun ordinaryCardUsesNormalAodResources() {
        assertEquals(0, rules.layer(false, 1, false))
        assertEquals("normal", rules.layerName(0))
        assertEquals("notification_panoramic_aod_bg", rules.backgroundName(0))
    }

    @Test fun stackedCardsUseMatchingNativeLayers() {
        assertEquals(1, rules.layer(true, 1, false))
        assertEquals(2, rules.layer(true, 2, false))
        assertEquals("first", rules.layerName(1))
        assertEquals("second", rules.layerName(2))
        assertEquals("notification_panoramic_aod_bg_first_stack", rules.backgroundName(1))
        assertEquals("notification_panoramic_aod_bg_second_stack", rules.backgroundName(2))
    }

    @Test fun overlayUsesSecondLayerRegardlessOfStackState() {
        for (stacked in listOf(false, true)) for (type in 0..3) {
            assertEquals(2, rules.layer(stacked, type, true))
        }
    }

    @Test fun unknownStacksFallBackToNormalMaterial() {
        for (type in listOf(-1, 0, 3, Int.MAX_VALUE)) {
            assertEquals(0, rules.layer(true, type, false))
            assertEquals("normal", rules.layerName(type))
            assertEquals("notification_panoramic_aod_bg", rules.backgroundName(type))
        }
    }

    @Test fun featureOffOrdinaryRowsAndAwakeStateNeverMatch() {
        assertTrue(rules.use(true, true, 1f))
        assertFalse(rules.use(false, true, 1f))
        assertFalse(rules.use(true, false, 1f))
        assertFalse(rules.use(true, true, 0f))
        assertFalse(rules.use(true, true, Float.NaN))
    }

    @Test fun unsupportedShaderMixesKeepOpaqueAodFallback() {
        for (name in listOf("BlurMixSingle", "BlurMixMulti", "BlurMixMultiWithShader")) {
            assertTrue(rules.supportsShaderMix(name))
        }
        for (name in listOf(null, "None", "OverlayColor", "BlurMixSingleWithShader", "UNKNOWN")) {
            assertFalse(rules.supportsShaderMix(name))
        }
    }

    @Test fun awakeFramesDoNotOverwriteNativeReferenceEffectAlpha() {
        assertFalse(rules.needsEffectUpdate(true, null, 0f))
        assertFalse(rules.needsEffectUpdate(true, 0f, 0f))
        assertFalse(rules.needsEffectUpdate(false, 0f, 0f))
    }

    @Test fun effectsFadeOnEntryAndRestoreOnExit() {
        assertTrue(rules.needsEffectUpdate(false, 0f, 0.5f))
        assertTrue(rules.needsEffectUpdate(false, 0.5f, 1f))
        assertTrue(rules.needsEffectUpdate(false, 1f, 0.5f))
        assertTrue(rules.needsEffectUpdate(false, 0.5f, 0f))
        assertFalse(rules.needsEffectUpdate(false, 1f, 1f))
    }

    @Test fun newlyAddedAodCardAndStyleChangeApplyEffectsImmediately() {
        assertTrue(rules.needsEffectUpdate(false, null, 1f))
        assertTrue(rules.needsEffectUpdate(true, 1f, 1f))
    }

    @Test fun nativeMaterialTintIsRetainedUnlessItWouldCoverAnUnhandledMask() {
        for (handled in listOf(false, true)) {
            assertTrue(rules.drawMaterialTint(0f, handled))
        }
        for (fraction in listOf(0.01f, 0.5f, 1f)) {
            assertFalse(rules.drawMaterialTint(fraction, false))
            // Match native shader rows; fallback draws its opaque AOD mask after this tint.
            assertTrue(rules.drawMaterialTint(fraction, true))
        }
    }

    @Test fun rendererSwitchRetainsRealNativeMaskNotPreviousReplacement() {
        val original = Any()
        val replacement = Any()
        assertSame(original, rules.originalMask(replacement, replacement, original))
        assertNull(rules.originalMask(replacement, replacement, null))
    }

    @Test fun maskRestorationDoesNotOverrideNewNativeMask() {
        val oldOriginal = Any()
        val replacement = Any()
        val nativeUpdate = Any()
        assertSame(nativeUpdate, rules.originalMask(nativeUpdate, replacement, oldOriginal))
        assertNull(rules.originalMask(null, replacement, oldOriginal))
        assertSame(nativeUpdate, rules.originalMask(nativeUpdate, null, oldOriginal))
    }
}
