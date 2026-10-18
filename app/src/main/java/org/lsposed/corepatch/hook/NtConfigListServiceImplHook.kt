// Adapted from LSPosed/CorePatch, commit ddf87bb5fb2bb2eb962992103abf0b225952d738.
// Upstream declares GNU GPL v2.0. See third_party/corepatch/NOTICE.txt.
package org.lsposed.corepatch.hook

import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object NtConfigListServiceImplHook : BaseHook() {
    override val name = "NtConfigListServiceImplHook"

    override fun hook() {
        // TODO: Check is Nothing Phone
        val ntConfigListServiceImplClazz = try {
            hostClassLoader.loadClass("com.nothing.server.ex.NtConfigListServiceImpl")
        } catch (e: ClassNotFoundException) {
            return
        }
        val isInstallingAppForbiddenMethod =
            ntConfigListServiceImplClazz.declaredMethods.first { m -> m.name == "isInstallingAppForbidden" }
        hookBefore(isInstallingAppForbiddenMethod) { callback ->
            if (Config.isBypassBlockEnabled()) callback.returnAndSkip(false)
        }
        val isStartingAppForbiddenMethod =
            ntConfigListServiceImplClazz.declaredMethods.first { m -> m.name == "isStartingAppForbidden" }
        hookBefore(isStartingAppForbiddenMethod) { callback ->
            if (Config.isBypassBlockEnabled()) callback.returnAndSkip(false)
        }
    }
}
