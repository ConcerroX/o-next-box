package com.mi.onextbox.lsp

/** Sparse evidence, not a heartbeat or a claim about the current configuration. */
internal object HookEvidence {
    fun report(feature: String, state: String) {
        HookLog.i("HookEvidence", "feature=$feature state=$state pid=${android.os.Process.myPid()}")
    }
}
