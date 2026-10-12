package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** OEM views are unavailable on the JVM; guard their generation-ownership boundary explicitly. */
class C15AboutOtaHotReloadContractTest {
    @Test fun capturesHostRootsAndLoadersInsteadOfModuleCardState() {
        val source = source()
        assertTrue(source.contains("WeakHashMap<ViewGroup, ClassLoader>()"))
        val capture = source.substringAfter("register(\"settings:c15-ota\", capture = {")
            .substringBefore("}, detach = {")
        assertTrue(capture.contains("arrayOf(root, loader)"))
        assertFalse(capture.contains("CardState"))
        assertFalse(capture.contains(".tag"))
    }

    @Test fun retirementRestoresNativeCardAndReplacementBindsNewGeneration() {
        val source = source()
        assertTrue(source.contains("boundRoots.keys.toList().forEach(::clearOverlay)"))
        assertTrue(source.contains("boundRoots.clear()"))
        assertTrue(source.contains("bindCard(root, loader, LspConfig.isSettingsC15AboutLayoutEnabledXposed())"))
        assertTrue(source.contains("boundRoots[root] = classLoader"))
        val clear = source.substringAfter("private fun clearOverlay(root: ViewGroup)")
            .substringBefore("private fun findOverlay")
        assertTrue(clear.contains("restoreNativeViews(root, card, state)"))
        assertTrue(clear.indexOf("overlay.tag = null") < clear.indexOf("card.removeView(overlay)"))
    }

    @Test fun everyRemovedOverlayDropsItsModuleOwnedTag() {
        val source = source()
        val removals = source.split("card.removeView(overlay)").dropLast(1)
        assertTrue(removals.size == 3)
        removals.forEach { beforeRemoval ->
            assertTrue(beforeRemoval.trimEnd().endsWith("overlay.tag = null"))
        }
    }

    private fun source(): String = listOf(
        File("src/main/java/com/mi/onextbox/lsp/C15AboutOtaHooker.kt"),
        File("app/src/main/java/com/mi/onextbox/lsp/C15AboutOtaHooker.kt"),
    ).first { it.isFile }.readText()
}
