package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Installation guards for hooks whose OEM targets cannot be loaded on the JVM. */
class LauncherRecentInteractionHookContractTest {
    @Test fun disabledFeaturesReturnBeforeResolvingLauncherOrRegisteringHooks() {
        val entry = source().substringAfter("fun hook(loader: ClassLoader)")
            .substringBefore("private fun hookAutoFocus")
        val guard = entry.indexOf(
            "if (!enabled(LauncherFeature.DisablePreviousTaskAutoFocus) &&\n" +
                "            !enabled(LauncherFeature.RecentIconAppDetails)\n" +
                "        ) return",
        )
        assertTrue("Both disabled features must skip the bootstrap", guard >= 0)
        assertTrue(guard < entry.indexOf("install(\"startup\")"))
        assertTrue(guard < entry.indexOf("ModernReflect.findClass("))
        assertTrue(guard < entry.indexOf("HotReloadLifecycle.registerReplay("))
    }

    @Test fun enablingAppDetailsAloneDoesNotInstallOrDeoptimizeAutoFocus() {
        assertTrue(source().contains(
            "if (enabled(LauncherFeature.DisablePreviousTaskAutoFocus)) {\n" +
                "                    install(\"auto-focus\") { hookAutoFocus(loader) }\n" +
                "                }",
        ))
        val bootstrap = source().substringBefore("private fun hookAutoFocus")
        assertTrue("Deoptimization must remain inside the guarded auto-focus installer",
            !bootstrap.contains("deoptimize("))
    }

    @Test fun enablingAutoFocusAloneDoesNotInstallTouchHooksAndReloadRetainsTheSameGuards() {
        assertTrue(source().contains(
            "if (enabled(LauncherFeature.RecentIconAppDetails)) {\n" +
                "                    install(\"app-details\") { hookAppDetails(loader) }\n" +
                "                }",
        ))
        assertTrue(source().contains("if (saved == true) installReadyHooks()"))
        assertTrue(source().contains("if (installed || HotReloadLifecycle.isRetiring) return"))
    }

    private fun source(): String = listOf(
        File("src/main/java/com/mi/onextbox/lsp/LauncherRecentInteractionHooker.kt"),
        File("app/src/main/java/com/mi/onextbox/lsp/LauncherRecentInteractionHooker.kt"),
    ).first { it.isFile }.readText().replace("\r\n", "\n")
}
