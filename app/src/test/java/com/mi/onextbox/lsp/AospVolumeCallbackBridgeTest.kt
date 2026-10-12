package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class AospVolumeCallbackBridgeTest {
    @Test fun onlyNativeProducerReceivesNonNullState() {
        assertTrue(AospVolumeCallbackBridge.shouldForward(true, true))
        assertFalse(AospVolumeCallbackBridge.shouldForward(false, true))
        assertFalse(AospVolumeCallbackBridge.shouldForward(true, false))
        assertFalse(AospVolumeCallbackBridge.shouldForward(false, false))
    }

    @Test fun bridgeDoesNotReplaceExistingOverridesOrRedispatchToAllCallbacks() {
        val source = listOf(File("src/main/java/com/mi/onextbox/lsp/AospVolumeCallbackBridge.kt"),
            File("app/src/main/java/com/mi/onextbox/lsp/AospVolumeCallbackBridge.kt"))
            .first { it.isFile }.readText()
        assertTrue(source.contains("method.declaringClass != callbacks"))
        assertTrue(source.contains("update.invoke(target, snapshot)"))
        assertTrue(source.contains("handler.post"))
        assertTrue(source.contains("listeners[target] === handler"))
        assertTrue(source.contains("copy.invoke(source)"))
        assertFalse(source.contains("getDeclaredMethod(\"onInitStateChange\""))
        assertFalse(source.contains("getState()"))
    }
}
