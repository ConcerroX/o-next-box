package com.mi.onextbox.lsp

/** A bounded intent from the keyguard's own UDFPS callback, never authentication by itself. */
internal class FaceTapNativeHandoff {
    companion object {
        const val RELEASE_GRACE_MS = 500L
        const val MAX_HELD_MS = 5_000L
        const val POLL_MS = 32L
    }

    private var user: Int? = null
    private var downAt = 0L
    private var releasedAt = 0L
    private var valid = false
    var held = false
        private set
    val pending: Boolean get() = user != null && valid

    fun pointerDown(time: Long, state: FaceTapUnlockRules.State?) {
        // A duplicate sensor DOWN cannot extend the deadline or re-arm an invalidated contact.
        if (held) return
        reset()
        if (time <= 0L || state == null || !state.showing || state.handoffBlocked ||
            state.faceAuthenticated || !state.faceAllowed) return
        user = state.currentUser
        downAt = time
        held = true
        valid = true
    }

    fun pointerUp(time: Long, state: FaceTapUnlockRules.State?) {
        if (!held) return
        held = false
        touchReleased(time)
        observeState(time, state)
    }

    /** An earlier window UP bounds the grace period even if the sensor UP arrives late. */
    fun touchReleased(time: Long) {
        if (!pending) return
        if (time < downAt || time - downAt > MAX_HELD_MS) {
            invalidate()
        } else if (releasedAt == 0L) {
            releasedAt = time
        }
    }

    fun windowCanceled(state: FaceTapUnlockRules.State?) {
        if (state == null || state.currentUser != user || !state.showing || state.handoffBlocked ||
            !state.faceAllowed) {
            invalidate()
            return
        }
        // A real sensor contact survives disposal of the optical window. A cancellation without
        // that independent evidence cannot become a recent-release intent.
        if (!held && !(releasedAt > 0L && state.faceAuthenticated)) invalidate()
    }

    fun observeState(now: Long, state: FaceTapUnlockRules.State?) {
        if (!pending) return
        if (state == null || state.currentUser != user || !state.showing || state.handoffBlocked ||
            !state.faceAllowed || expired(now)) invalidate()
    }

    fun claim(now: Long, authenticatedAt: Long, state: FaceTapUnlockRules.State?): Boolean {
        observeState(now, state)
        if (!pending || state == null || !FaceTapUnlockRules.eligible(state) ||
            authenticatedAt < downAt || authenticatedAt <= 0L || now < authenticatedAt) return false
        // All authorization is re-read by the caller again immediately before the stock dismiss.
        invalidate()
        return true
    }

    fun invalidate() { valid = false }

    fun reset() {
        user = null
        downAt = 0L
        releasedAt = 0L
        held = false
        valid = false
    }

    private fun expired(now: Long): Boolean = now < downAt ||
        if (releasedAt > 0L) now < releasedAt || now - releasedAt > RELEASE_GRACE_MS
        else !held || now - downAt > MAX_HELD_MS
}
