package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class HideRecentAppsTest {
    @Test fun onlyExactSelectedPackagesAreHidden() {
        assertFalse(HideRecentAppsRules.hideGroup(listOf("a.b"), emptySet()))
        assertFalse(HideRecentAppsRules.hideGroup(listOf(null, "", "a.bc"), setOf("a.b")))
        assertTrue(HideRecentAppsRules.hideGroup(listOf("a.b"), setOf("a.b")))
    }
    @Test fun splitPairsAreFilteredAsOnePresentationCard() {
        assertTrue(HideRecentAppsRules.hideGroup(listOf("a.b", "c.d"), setOf("c.d")))
        assertTrue(HideRecentAppsRules.hideGroup(listOf("a.b", "c.d"), setOf("a.b")))
        assertFalse(HideRecentAppsRules.hideGroup(emptyList(), setOf("a.b")))
    }
    @Test fun implementationNeverRemovesNativeTasksOrChangesCache() {
        val source = File("src/main/java/com/mi/onextbox/lsp/HideRecentAppsHooker.kt").readText()
        assertTrue(source.contains("param.result = ArrayList(filtered)"))
        assertTrue(source.contains("original.filterNot"))
        assertTrue(source.contains("if (hidden.isEmpty()) return"))
        assertTrue(source.contains("\"shouldAddStubTaskView\", taskArray"))
        assertTrue(source.contains("if (HideRecentAppsRules.hideGroup(packages, hidden)) param.result = false"))
        for (unsafe in listOf("removeTask(", "forceStopPackage", "removeAll(", "clear()", "setExcludeFromRecents"))
            assertFalse(unsafe, source.contains(unsafe))
    }
}
