package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** COUI host classes are unavailable in JVM tests; guard the verified teardown contract. */
class SettingsNativePanelReloadContractTest {
    @Test fun disposalBypassesTheNullRejectingCouiAdapterOverloads() {
        val detach = source().substringAfter("private fun detachAdapter() {")
            .substringBefore("private fun refreshAll()")
        assertTrue(detach.contains("type(\"androidx.recyclerview.widget.RecyclerView\")"))
        assertTrue(detach.contains(".setType(XposedInterface.Invoker.Type.ORIGIN)"))
        assertTrue(detach.contains(".invokeSpecial(list, *arrayOfNulls<Any>(1))"))
        assertFalse(detach.contains(".invoke(list,"))
        assertFalse(detach.contains("call(list, \"setAdapter\""))
        assertFalse(detach.contains("call(oldConnector, \"setExpandableListAdapter\""))
    }

    @Test fun nativeRecyclingPrecedesClearingTheOldAdapterAndConnector() {
        val detach = source().substringAfter("private fun detachAdapter() {")
            .substringBefore("private fun refreshAll()")
        val recycling = detach.indexOf(".invokeSpecial(list,")
        val unregister = detach.indexOf("call(oldAdapter, \"unregisterAdapterDataObserver\", observer)")
        val clearProxy = detach.indexOf("setObjectField(oldConnector, \"mExpandableListAdapter\", null)")
        val clearAdapter = detach.indexOf("setObjectField(list, \"mAdapter\", null)")
        val clearConnector = detach.indexOf("setObjectField(list, \"mConnector\", null)")
        assertTrue(recycling >= 0 && unregister > recycling)
        assertTrue(clearProxy > unregister && clearAdapter > clearProxy)
        assertTrue(clearConnector > clearAdapter)
        assertTrue(detach.indexOf("view.removeView(list)") > clearConnector)
    }

    private fun source(): String = listOf(
        File("src/main/java/com/mi/onextbox/lsp/SettingsNativeAppInfoPanel.kt"),
        File("app/src/main/java/com/mi/onextbox/lsp/SettingsNativeAppInfoPanel.kt"),
    ).first { it.isFile }.readText()
}
