package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class PowerIndicatorRulesTest {
    @Test fun unitsAndCellCorrectionAreExplicit() {
        assertEquals(4.0, PowerIndicatorRules.watts(-1_000_000, 4000, 0, 0)!!, 0.0001)
        assertEquals(4.0, PowerIndicatorRules.watts(-1000, 4000, 1, 0)!!, 0.0001)
        assertEquals(8.0, PowerIndicatorRules.watts(1_000_000, 4000, 2, 1)!!, 0.0001)
        assertEquals(2.0, PowerIndicatorRules.watts(1_000_000, 4000, 0, 2)!!, 0.0001)
        assertEquals(0.004, PowerIndicatorRules.watts(1000, 4000, 0, 0)!!, 0.0001)
    }
    @Test fun invalidAndUnsupportedReadingsAreNotDisplayed() {
        assertNull(PowerIndicatorRules.watts(Long.MIN_VALUE, 4000, 0, 0))
        assertNull(PowerIndicatorRules.watts(Int.MIN_VALUE.toLong(), 4000, 0, 0))
        assertNull(PowerIndicatorRules.watts(Long.MAX_VALUE, 4000, 0, 0))
        assertNull(PowerIndicatorRules.watts(1_000_000, 0, 0, 0))
        assertNull(PowerIndicatorRules.watts(100_000_000, 4000, 0, 0))
        assertEquals(0.0, PowerIndicatorRules.watts(0, 4000, 0, 0)!!, 0.0)
    }
    @Test fun settingsHaveSafeDefaultsAndBounds() {
        val defaults = PowerIndicatorSettings.State()
        assertFalse(defaults.enabled)
        assertTrue(defaults.hideCharging)
        assertEquals(0, defaults.scale)
        val s = PowerIndicatorSettings.normalize(defaults.copy(interval = -1, decimals = 10, scale = -1, source = 99, fontSize = 99))
        assertEquals(500, s.interval)
        assertEquals(2, s.decimals)
        assertEquals(0, s.scale)
        assertEquals(2, s.source)
        assertEquals(16, s.fontSize)
    }
    @Test fun chargingAndPrecisionAreUnambiguous() {
        assertEquals("+4.00 W", PowerIndicatorRules.format(4.0, 2, true))
        assertEquals("4 W", PowerIndicatorRules.format(4.0, 0, false))
        assertEquals("4.3 W", PowerIndicatorRules.format(4.26, 1, false))
    }
}
