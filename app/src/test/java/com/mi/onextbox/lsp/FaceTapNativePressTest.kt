package com.mi.onextbox.lsp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceTapNativePressTest {
    private val waiting = FaceTapUnlockRules.State(null, 0, false, true, false, true, true, false)
    private val authenticated = waiting.copy(sessionUser = 0, faceAuthenticated = true, canSkipBouncer = true)
    private val area = FaceTapUnlockRules.IconBounds(80, 180, 180, 280)

    private fun press() = FaceTapUnlockRules.NativePress().apply {
        begin(100f, 200f, 1000L, waiting, area)
    }

    @Test fun nativePressCanFinishAfterFaceWinsWithoutAnotherDown() {
        val press = press()
        assertTrue(press.finish(100f, 200f, 1300L, 1200L, authenticated))
        assertFalse(press.finish(100f, 200f, 1400L, 1200L, authenticated))
    }

    @Test fun queuedReleaseBeforeFaceCannotDismiss() {
        assertFalse(press().finish(100f, 200f, 1100L, 1200L, authenticated))
        assertFalse(press().finish(100f, 200f, 1300L, 0L, authenticated))
        assertFalse(press().finish(100f, 200f, 1300L, 900L, authenticated))
    }

    @Test fun liveAuthorizationAndSameUserAreRequired() {
        listOf(null, waiting, authenticated.copy(faceAllowed = false),
            authenticated.copy(canSkipBouncer = false), authenticated.copy(blocked = true),
            authenticated.copy(showing = false), authenticated.copy(interactive = false),
            authenticated.copy(sessionUser = 10, currentUser = 10)).forEach {
            assertFalse(press().finish(100f, 200f, 1300L, 1200L, it))
        }
    }

    @Test fun movementCancellationAndDuplicateDownCannotRearmContact() {
        val press = press()
        press.move(200f, 200f)
        press.begin(100f, 200f, 1000L, waiting, area)
        assertFalse(press.finish(100f, 200f, 1300L, 1200L, authenticated))
        val invalid = press()
        invalid.invalidate()
        assertFalse(invalid.finish(100f, 200f, 1300L, 1200L, authenticated))
        val canceled = press()
        canceled.reset()
        assertFalse(canceled.finish(100f, 200f, 1300L, 1200L, authenticated))
    }

    @Test fun blockedOrAlreadyAuthenticatedDownDoesNotBecomeNativeHandoff() {
        listOf(null, authenticated, waiting.copy(blocked = true), waiting.copy(showing = false),
            waiting.copy(interactive = false)).forEach { state ->
            val press = FaceTapUnlockRules.NativePress()
            press.begin(100f, 200f, 1000L, state, area)
            assertFalse(press.active)
            assertFalse(press.finish(100f, 200f, 1300L, 1200L, authenticated))
        }
    }

    @Test fun releaseMustStayInsideNativeIcon() {
        assertFalse(press().finish(200f, 200f, 1300L, 1200L, authenticated))
        assertFalse(press().finish(Float.NaN, 200f, 1300L, 1200L, authenticated))
        val press = press()
        press.move(145f, 235f)
        assertTrue(press.finish(145f, 235f, 1300L, 1200L, authenticated))
    }
}
