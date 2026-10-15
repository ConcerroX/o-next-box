package com.mi.onextbox.lsp

import com.mi.onextbox.lsp.LauncherBlurRules.Effect
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors

class LauncherBlurRulesTest {
    private val enabled = LauncherBlurRules.State(true, true, false, false, false)

    @Test fun absentAnimationSettingUsesNativeDefaultButOtherValuesDoNotEnableBlur() {
        assertTrue(LauncherBlurRules.animationEnabled(-1))
        assertTrue(LauncherBlurRules.animationEnabled(1))
        listOf(Int.MIN_VALUE, -2, 0, 2, Int.MAX_VALUE).forEach {
            assertFalse(LauncherBlurRules.animationEnabled(it))
        }
    }

    @Test fun nativeAnimationAndMaterialSwitchesRemainIndependent() {
        assertTrue(enabled.allows(Effect.Animation))
        assertTrue(enabled.allows(Effect.Material))
        val animationOff = enabled.copy(animation = false)
        assertFalse(animationOff.allows(Effect.Animation))
        assertTrue(animationOff.allows(Effect.Material))
        val materialOff = enabled.copy(material = false)
        assertTrue(materialOff.allows(Effect.Animation))
        assertFalse(materialOff.allows(Effect.Material))
    }

    @Test fun allProtectiveStatesPreventForcingEitherBlurPath() {
        listOf(enabled.copy(lowPower = true), enabled.copy(superPowerSave = true),
            enabled.copy(windowBlursDisabled = true)).forEach { state ->
            Effect.entries.forEach { assertFalse(state.allows(it)) }
        }
    }

    @Test fun nativeBlurCanResumeAfterPowerSavingWithoutChangingModulePreference() {
        assertFalse(enabled.copy(lowPower = true).allows(Effect.Animation))
        assertTrue(enabled.allows(Effect.Animation))
        assertFalse(enabled.copy(superPowerSave = true).allows(Effect.Material))
        assertTrue(enabled.allows(Effect.Material))
    }

    @Test fun tierOverrideIsNestedAndClearedAfterBothSuccessAndFailure() {
        val scope = LauncherBlurScope()
        assertFalse(scope.isActive())
        scope.within {
            assertTrue(scope.isActive())
            scope.within { assertTrue(scope.isActive()) }
            assertTrue(scope.isActive())
            runCatching { scope.within<Unit> { error("native failure") } }
            assertTrue(scope.isActive())
        }
        assertFalse(scope.isActive())
        runCatching { scope.within<Unit> { error("native failure") } }
        assertFalse(scope.isActive())
        assertEquals("native result", scope.within { "native result" })
        assertFalse(scope.isActive())
    }

    @Test fun tierOverrideNeverLeaksToOtherThreads() {
        val scope = LauncherBlurScope()
        val executor = Executors.newSingleThreadExecutor()
        try {
            scope.within {
                assertFalse(executor.submit<Boolean> { scope.isActive() }.get())
                assertTrue(executor.submit<Boolean> { scope.within { scope.isActive() } }.get())
                assertFalse(executor.submit<Boolean> { scope.isActive() }.get())
                assertTrue(scope.isActive())
            }
        } finally { executor.shutdownNow() }
    }
}
