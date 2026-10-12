package com.mi.onextbox.ui.common

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LspMissingScopesNoticeUiContractTest {
    private val source = listOf(
        File("src/main/java/com/mi/onextbox/ui/common/LspMissingScopesNotice.kt"),
        File("app/src/main/java/com/mi/onextbox/ui/common/LspMissingScopesNotice.kt"),
    ).first { it.isFile }.readText()

    @Test fun materialActionRetainsNativeTextButtonPaddingAndPressFeedback() {
        val action = source.substringAfter("androidx.compose.material3.TextButton(").substringBefore("} else {")
        assertTrue(action.contains("onClick = onOpen"))
        assertTrue(action.contains("enabled = !opening"))
        assertFalse(action.contains("contentPadding ="))
        assertFalse(action.contains("indication = null"))
        assertFalse(action.contains("pointerInput"))
    }

    @Test fun materialActionUsesTheNoticeCardContentColor() {
        val action = source.substringAfter("androidx.compose.material3.TextButton(").substringBefore("} else {")
        assertTrue(action.contains("ButtonDefaults.textButtonColors("))
        assertTrue(action.contains("contentColor = MaterialTheme.colorScheme.onTertiaryContainer"))
    }
}
