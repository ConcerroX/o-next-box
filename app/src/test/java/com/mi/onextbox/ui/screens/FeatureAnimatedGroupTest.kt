package com.mi.onextbox.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureAnimatedGroupTest {
    private fun keys(parent: Boolean, child: Boolean): List<Any> =
        FeatureAnimatedGroupScope().apply {
            row("main") { _, _ -> }
            row("child", visible = parent) { _, _ -> }
            row("nested", visible = parent && child) { _, _ -> }
            row("independent") { _, _ -> }
        }.rows.map { it.first }

    @Test fun disabledParentLeavesNoChildSlotsEvenWithSavedChildEnabled() {
        assertEquals(listOf("main", "independent"), keys(false, true))
        assertEquals(listOf("main", "independent"), keys(false, false))
    }

    @Test fun childrenReturnInStableOrderWhenParentReopens() {
        assertEquals(listOf("main", "child", "nested", "independent"), keys(true, true))
        assertEquals(listOf("main", "child", "independent"), keys(true, false))
    }
}
