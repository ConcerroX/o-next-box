package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class HideNotificationClearContractTest {
    @Test fun defaultsOffAndUsesNativeHideInsteadOfDroppingLifecycle() {
        val source = File("src/main/java/com/mi/onextbox/lsp/HideNotificationClearHooker.kt").readText()
        assertTrue(source.contains("getBoolean(KEY, false)"))
        assertTrue(source.contains("HookConfigSnapshot.boolean(KEY, false)"))
        assertTrue(source.contains("param.args[1] = false"))
        assertTrue(source.contains("param.args[2] = false"))
        assertFalse(source.contains("param.result ="))
        assertFalse(source.contains("setOnClickListener"))
    }
}
