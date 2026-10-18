// Adapted from LSPosed/CorePatch, commit ddf87bb5fb2bb2eb962992103abf0b225952d738.
// Upstream declares GNU GPL v2.0. See third_party/corepatch/NOTICE.txt.
package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object AssetManagerHook : BaseHook() {
    override val name = "AssetManagerHook"

    @SuppressLint("BlockedPrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val assetManagerClazz = hostClassLoader.loadClass("android.content.res.AssetManager")

        // Targeting R+ (version " + Build.VERSION_CODES.R + " and above) requires"
        // + " the resources.arsc of installed APKs to be stored uncompressed"
        // + " and aligned on a 4-byte boundary
        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/core/java/android/content/res/AssetManager.java;l=828
        // public boolean containsAllocatedTable()
        val containsAllocatedTableMethod =
            assetManagerClazz.getDeclaredMethod("containsAllocatedTable")
        hookBefore(containsAllocatedTableMethod) { callback ->
            if (Config.isBypassResourceArscRestrictionsEnabled()) {
                callback.returnAndSkip(false)
            }
        }
    }
}
