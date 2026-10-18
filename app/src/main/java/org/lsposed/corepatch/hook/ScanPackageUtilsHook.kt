// Adapted from LSPosed/CorePatch, commit ddf87bb5fb2bb2eb962992103abf0b225952d738.
// Upstream declares GNU GPL v2.0. See third_party/corepatch/NOTICE.txt.
package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object ScanPackageUtilsHook : BaseHook() {
    override val name = "ScanPackageUtilsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val scanPackageUtilsClazz =
            hostClassLoader.loadClass("com.android.server.pm.ScanPackageUtils")
        val assertMinSignatureSchemeIsValidMethod =
            scanPackageUtilsClazz.declaredMethods.first { m -> m.name == "assertMinSignatureSchemeIsValid" }
        hookBefore(assertMinSignatureSchemeIsValidMethod) { callback ->
            if (Config.isBypassVerificationEnabled()) {
                callback.returnAndSkip(null)
            }
        }
    }
}
