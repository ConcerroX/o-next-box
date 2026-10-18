// Adapted from LSPosed/CorePatch, commit ddf87bb5fb2bb2eb962992103abf0b225952d738.
// Upstream declares GNU GPL v2.0. See third_party/corepatch/NOTICE.txt.
package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object VerificationParamsHook: BaseHook() {
    override val name = "VerificationParamsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val verificationParamsClazz = hostClassLoader.loadClass("com.android.server.pm.VerificationParams")

        val isVerificationEnabledMethod = verificationParamsClazz.declaredMethods.first { m -> m.name == "isVerificationEnabled" }
        hookBefore(isVerificationEnabledMethod) { callback ->
            if (Config.isDisableVerificationAgentEnabled()) {
                callback.returnAndSkip(false)
            }
        }
    }
}
