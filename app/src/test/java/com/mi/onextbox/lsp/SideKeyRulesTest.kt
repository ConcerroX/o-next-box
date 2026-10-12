package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class SideKeyRulesTest {
    private val machine = SideKeyGestureMachine()
    private val single = SideKeyBinding(SideKeyAction.WeChatScan)
    private val double = SideKeyBinding(SideKeyAction.AlipayPay)
    private val long = SideKeyBinding(SideKeyAction.Flashlight)
    private val configured = SideKeyConfig(single, double, long)

    @Test fun defaultsPreserveNativeTapAndLongPress() {
        assertTrue(machine.down(100, SideKeyConfig()).isEmpty())
        assertEquals(listOf(SideKeyCommand.StockTap), machine.up(150))
        machine.down(200, SideKeyConfig())
        assertEquals(listOf(SideKeyCommand.StockLongDown), machine.advance(695))
        assertTrue(machine.advance(900).isEmpty())
        assertEquals(listOf(SideKeyCommand.StockLongUp), machine.up(1000))
        assertTrue(machine.isIdle)
    }

    @Test fun singlePressExecutesImmediatelyWithoutDoubleAction() {
        machine.down(0, configured.copy(double = SideKeyBinding(SideKeyAction.None)))
        assertEquals(listOf(SideKeyCommand.Run(single)), machine.up(50))
        assertNull(machine.nextDeadline)
    }

    @Test fun singleWaitsForDoubleWindowAndFiresExactlyOnce() {
        machine.down(0, configured)
        assertTrue(machine.up(40).isEmpty())
        assertEquals(340L, machine.nextDeadline)
        assertTrue(machine.advance(339).isEmpty())
        assertEquals(listOf(SideKeyCommand.Run(single)), machine.advance(340))
        assertTrue(machine.advance(500).isEmpty())
        assertTrue(machine.isIdle)
    }

    @Test fun doublePressSuppressesBothSingleActions() {
        machine.down(0, configured)
        machine.up(40)
        assertTrue(machine.down(200, configured).isEmpty())
        assertEquals(listOf(SideKeyCommand.Run(double)), machine.up(250))
        assertTrue(machine.advance(1000).isEmpty())
        assertTrue(machine.isIdle)
    }

    @Test fun secondPressStartingAtWindowBoundaryIsNotDouble() {
        machine.down(0, configured)
        machine.up(40)
        assertEquals(listOf(SideKeyCommand.Run(single)), machine.down(340, configured))
        machine.up(400)
        assertEquals(listOf(SideKeyCommand.Run(single)), machine.advance(700))
    }

    @Test fun longPressDoesNotBecomeATapOrDouble() {
        machine.down(0, configured)
        assertEquals(495L, machine.nextDeadline)
        assertTrue(machine.advance(494).isEmpty())
        assertEquals(listOf(SideKeyCommand.Run(long)), machine.advance(495))
        assertTrue(machine.advance(600).isEmpty())
        assertTrue(machine.up(700).isEmpty())
        assertTrue(machine.isIdle)
    }

    @Test fun releaseAfterThresholdRecognizesLongEvenIfTimerWasLate() {
        machine.down(0, configured)
        assertEquals(listOf(SideKeyCommand.Run(long)), machine.up(600))
    }

    @Test fun lateTimerForStockLongStillProducesBalancedEvents() {
        machine.down(0, SideKeyConfig())
        assertEquals(listOf(SideKeyCommand.StockLongDown, SideKeyCommand.StockLongUp), machine.up(700))
    }

    @Test fun secondPressHeldLongRetainsFirstTapAndRunsLongInsteadOfDouble() {
        machine.down(0, configured)
        machine.up(50)
        machine.down(200, configured)
        assertEquals(listOf(SideKeyCommand.Run(single), SideKeyCommand.Run(long)), machine.advance(695))
        assertTrue(machine.up(800).isEmpty())
        assertTrue(machine.isIdle)
    }

    @Test fun cancelledPressProducesNoAction() {
        machine.down(0, configured)
        assertTrue(machine.up(700, cancelled = true).isEmpty())
        assertTrue(machine.isIdle)
    }

    @Test fun cancelledNativeLongReleasesItsDownEvent() {
        machine.down(0, SideKeyConfig())
        machine.advance(500)
        assertEquals(listOf(SideKeyCommand.StockLongUp), machine.up(600, cancelled = true))
    }

    @Test fun cancelledSecondPressPreservesCompletedFirstTap() {
        machine.down(0, configured)
        machine.up(50)
        machine.down(100, configured)
        assertTrue(machine.up(200, cancelled = true).isEmpty())
        assertEquals(listOf(SideKeyCommand.Run(single)), machine.advance(350))
    }

    @Test fun repeatedDownDoesNotResetLongThreshold() {
        machine.down(0, configured)
        machine.down(100, configured)
        assertEquals(495L, machine.nextDeadline)
        assertEquals(listOf(SideKeyCommand.Run(long)), machine.advance(495))
    }

    @Test fun changedConfigurationCannotMergeUnrelatedTaps() {
        machine.down(0, configured)
        machine.up(50)
        val updated = configured.copy(single = SideKeyBinding(SideKeyAction.Screenshot))
        assertEquals(listOf(SideKeyCommand.Run(single)), machine.down(100, updated))
        machine.up(150)
        assertEquals(listOf(SideKeyCommand.Run(updated.single)), machine.advance(450))
    }

    @Test fun noneBindingsConsumeGesturesWithoutCommands() {
        val none = SideKeyBinding(SideKeyAction.None)
        machine.down(0, SideKeyConfig(none, none, none))
        assertTrue(machine.up(30).isEmpty())
        machine.down(100, SideKeyConfig(none, none, none))
        assertTrue(machine.advance(600).isEmpty())
        assertTrue(machine.up(700).isEmpty())
        assertTrue(machine.isIdle)
    }

    @Test fun resetClearsQueuedAndHeldGestures() {
        machine.down(0, configured)
        machine.up(50)
        machine.reset()
        assertTrue(machine.isIdle)
        assertNull(machine.nextDeadline)
        assertTrue(machine.advance(1000).isEmpty())
        machine.down(2000, configured)
        machine.reset()
        assertTrue(machine.up(3000).isEmpty())
    }

    @Test fun inputTimingAndPackageNamesAreValidated() {
        assertEquals(495, SideKeyRules.longInterval(-1))
        assertEquals(800, SideKeyRules.longInterval(800))
        assertEquals(300, SideKeyRules.doubleInterval(999))
        assertEquals(200, SideKeyRules.doubleInterval(200))
        listOf("com.tencent.mm", "com.eg.android.AlipayGphone", "a.b_c2").forEach {
            assertTrue(SideKeyRules.validPackage(it))
        }
        listOf("", "a", "a/b", "com.app;reboot", "com..app", "a." + "x".repeat(200)).forEach {
            assertFalse(SideKeyRules.validPackage(it))
        }
        assertEquals(SideKeyAction.None, SideKeyAction.parse("unknown"))
    }
}
