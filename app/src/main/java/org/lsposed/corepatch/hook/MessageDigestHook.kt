// Adapted from LSPosed/CorePatch, commit ddf87bb5fb2bb2eb962992103abf0b225952d738.
// Upstream declares GNU GPL v2.0. See third_party/corepatch/NOTICE.txt.
package org.lsposed.corepatch.hook

import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object MessageDigestHook : BaseHook() {
    override val name = "MessageDigestHook"

    override fun hook() {
        val messageDigestClazz = hostClassLoader.loadClass("java.security.MessageDigest")
        // https://cs.android.com/android/platform/superproject/main/+/main:libcore/ojluni/src/main/java/java/security/MessageDigest.java;l=518
        // public static boolean isEqual(byte[] digesta, byte[] digestb)
        val isEqualMethod = messageDigestClazz.getDeclaredMethod(
            "isEqual", ByteArray::class.java, ByteArray::class.java
        )
        XposedHelper.hookBefore(isEqualMethod) { callback ->
            if (Config.isBypassVerificationEnabled() &&
                Thread.currentThread().stackTrace.any {
                    it.className.startsWith("android.util.apk.") ||
                        it.className.startsWith("android.util.jar.")
                }) {
                callback.returnAndSkip(true)
            }
        }
    }
}
