package com.mi.onextbox.ui.settings

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class HookEvidenceSummaryContractTest {
    @Test fun sharedRowsCarrySegmentEdgesForBothUiStyles() {
        val source = File("src/main/java/com/mi/onextbox/ui/settings/HookEvidenceSummary.kt").readText()
        assertTrue(source.contains("hasDividerBelow = true"))
        assertTrue(source.contains("hasDividerAbove = true, hasDividerBelow = index < evidence.lastIndex"))
        assertTrue(source.contains("hook_evidence_notice"))
        assertTrue(source.contains("if (evidence.isEmpty()) return"))
    }
}
