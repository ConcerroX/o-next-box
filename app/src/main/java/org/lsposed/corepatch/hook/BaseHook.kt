// Adapted from LSPosed/CorePatch, commit ddf87bb5fb2bb2eb962992103abf0b225952d738.
// Upstream declares GNU GPL v2.0. See third_party/corepatch/NOTICE.txt.
package org.lsposed.corepatch.hook

import org.lsposed.corepatch.XposedHelper

open class BaseHook {
    open val name = "BaseHook"

    private var inited = false

    open fun hook() {

    }

    private fun hookInternal() {
        try {
            hook()
        } catch (t: Throwable) {
            XposedHelper.log("[$name] hook failed", t)
        }
    }

    fun init() {
        if (inited) return
        inited = true
        XposedHelper.log("[$name] init: $name")
        hookInternal()
        XposedHelper.log("[$name] init: $name done")
    }
}
