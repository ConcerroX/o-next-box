package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class FaceTapNativeHandoffTest {
    private val waiting = FaceTapUnlockRules.State(null, 0, false, true, false, true, true, false)
    private val authenticated = waiting.copy(sessionUser = 0, faceAuthenticated = true, canSkipBouncer = true)

    private fun held() = FaceTapNativeHandoff().apply { pointerDown(1000L, waiting) }
    private fun released() = held().apply { pointerUp(1100L, waiting) }

    @Test fun nativeContactCanEnterWhenFaceWinsBeforeFingerUpAndOnlyOnce() {
        val handoff = held()
        assertTrue(handoff.claim(1200L, 1200L, authenticated))
        assertFalse(handoff.claim(1210L, 1200L, authenticated))
        handoff.pointerDown(1250L, waiting) // Repeated DOWN without a real UP cannot re-arm it.
        assertFalse(handoff.pending)
        handoff.pointerUp(1300L, authenticated)
        assertFalse(handoff.claim(1310L, 1200L, authenticated))
    }

    @Test fun realReleaseBeforeFaceAuthenticationIsRetainedForTheShortHandoff() {
        val handoff = released()
        assertFalse(handoff.claim(1100L, 0L, waiting))
        assertTrue(handoff.pending)
        assertTrue(handoff.claim(1300L, 1250L, authenticated))
        assertFalse(handoff.pending)
    }

    @Test fun releasedContactExpiresAfterExactly500Milliseconds() {
        assertTrue(released().claim(1600L, 1600L, authenticated))
        val stale = released()
        assertFalse(stale.claim(1601L, 1601L, authenticated))
        assertFalse(stale.pending)
        assertFalse(stale.claim(1610L, 1610L, authenticated))
    }

    @Test fun delayedBouncerEligibilityDoesNotLoseAValidRecentRelease() {
        val handoff = released()
        assertFalse(handoff.claim(1200L, 1200L, authenticated.copy(canSkipBouncer = false)))
        assertTrue(handoff.pending)
        assertTrue(handoff.claim(1350L, 1200L, authenticated))
    }

    @Test fun aodPresentationFlagsMaySettleButCannotAuthorizeEntryWhileBlocked() {
        val dozing = waiting.copy(interactive = false, blocked = true, handoffBlocked = false)
        val handoff = FaceTapNativeHandoff()
        handoff.pointerDown(1000L, dozing)
        assertTrue(handoff.pending)
        val faceWhileDozing = authenticated.copy(interactive = false, blocked = true, handoffBlocked = false)
        assertFalse(handoff.claim(1200L, 1200L, faceWhileDozing))
        assertTrue(handoff.pending)
        assertFalse(handoff.claim(1250L, 1200L, authenticated.copy(blocked = true, handoffBlocked = false)))
        assertTrue(handoff.claim(1300L, 1200L, authenticated))
    }

    @Test fun securityRevocationPermanentlyInvalidatesThePendingContact() {
        listOf(
            authenticated.copy(blocked = true, handoffBlocked = true),
            authenticated.copy(faceAllowed = false),
            authenticated.copy(showing = false),
            authenticated.copy(currentUser = 10, sessionUser = 10),
        ).forEach { state ->
            val handoff = released()
            assertFalse(handoff.claim(1200L, 1200L, state))
            assertFalse(handoff.pending)
            assertFalse(handoff.claim(1250L, 1250L, authenticated))
        }
    }

    @Test fun cachedFaceOrTrustCannotSubstituteForFreshSameUserAuthentication() {
        listOf(
            authenticated.copy(faceAuthenticated = false),
            authenticated.copy(sessionUser = null),
            authenticated.copy(sessionUser = 10),
            authenticated.copy(canSkipBouncer = false),
            authenticated.copy(interactive = false),
        ).forEach { state -> assertFalse(released().claim(1200L, 1200L, state)) }
        assertFalse(released().claim(1200L, 0L, authenticated))
        assertFalse(released().claim(1200L, 900L, authenticated))
        assertFalse(released().claim(1200L, 1250L, authenticated))
        assertFalse(released().claim(1200L, 1200L, null))
    }

    @Test fun downWithAnAlreadyAuthenticatedFaceRemainsAnOrdinaryTap() {
        val handoff = FaceTapNativeHandoff()
        handoff.pointerDown(1000L, authenticated)
        assertFalse(handoff.pending)
        assertFalse(handoff.claim(1200L, 1200L, authenticated))
    }

    @Test fun opticalWindowCancellationCannotEraseAnIndependentHeldSensorContact() {
        val handoff = held()
        handoff.windowCanceled(waiting)
        assertTrue(handoff.pending)
        assertTrue(handoff.claim(1200L, 1200L, authenticated))
    }

    @Test fun cancellationWithoutAHeldSensorOrAuthWindowHandoffIsNotCached() {
        val handoff = released()
        handoff.windowCanceled(waiting)
        assertFalse(handoff.pending)
        assertFalse(handoff.claim(1250L, 1200L, authenticated))
        val authWindow = released()
        authWindow.windowCanceled(authenticated)
        assertTrue(authWindow.claim(1250L, 1200L, authenticated))
    }

    @Test fun cancellationDuringASecurityBlockCannotPreserveEvenAHeldSensorContact() {
        val handoff = held()
        handoff.windowCanceled(waiting.copy(blocked = true, handoffBlocked = true))
        assertFalse(handoff.pending)
        assertFalse(handoff.claim(1200L, 1200L, authenticated))
    }

    @Test fun motionExitMultitouchOrObscurationCannotBeRearmedByDuplicateDown() {
        val handoff = held()
        handoff.invalidate()
        handoff.pointerDown(1150L, waiting)
        assertFalse(handoff.pending)
        handoff.pointerUp(1200L, waiting)
        assertFalse(handoff.claim(1250L, 1250L, authenticated))
        handoff.pointerDown(1300L, waiting) // A genuinely new contact is distinct.
        assertTrue(handoff.claim(1400L, 1400L, authenticated))
    }

    @Test fun heldContactHasABoundedLifetimeEvenWhenSensorUpIsMissing() {
        assertTrue(held().claim(6000L, 6000L, authenticated))
        val expired = held()
        assertFalse(expired.claim(6001L, 6001L, authenticated))
        expired.pointerDown(6100L, waiting)
        assertFalse(expired.claim(6200L, 6200L, authenticated))
    }

    @Test fun lateReleaseCannotReviveAHoldWhichWasNeverPolled() {
        val expired = held()
        expired.pointerUp(6001L, waiting)
        assertFalse(expired.pending)
        assertFalse(expired.claim(6100L, 6100L, authenticated))
    }

    @Test fun duplicateDownAndUpCannotExtendTheHoldOrReleaseDeadline() {
        val handoff = held()
        handoff.pointerDown(1500L, waiting)
        handoff.pointerUp(1600L, waiting)
        handoff.pointerUp(1900L, waiting)
        assertFalse(handoff.claim(2101L, 2101L, authenticated))
        val duplicate = held()
        duplicate.pointerDown(4000L, waiting)
        assertFalse(duplicate.claim(6001L, 6001L, authenticated))
    }

    @Test fun windowReleaseStartsTheGraceEvenIfTheSensorReleaseIsLate() {
        val handoff = held()
        handoff.touchReleased(1100L)
        handoff.pointerUp(1400L, waiting)
        assertFalse(handoff.claim(1601L, 1601L, authenticated))
    }

    @Test fun resetDropsIntentAcrossSleepUserSwitchHostDetachAndReload() {
        val handoff = released()
        handoff.reset()
        handoff.pointerUp(1200L, waiting)
        assertFalse(handoff.pending)
        assertFalse(handoff.claim(1250L, 1250L, authenticated))
    }

    @Test fun orphanUpsAndInvalidClocksCannotCreateOrPreserveAnIntent() {
        val orphan = FaceTapNativeHandoff()
        orphan.pointerUp(1100L, waiting)
        assertFalse(orphan.claim(1200L, 1200L, authenticated))
        val reversed = held()
        reversed.pointerUp(999L, waiting)
        assertFalse(reversed.claim(1200L, 1200L, authenticated))
        val future = held()
        future.touchReleased(1300L)
        assertFalse(future.claim(1200L, 1200L, authenticated))
        assertFalse(held().claim(999L, 999L, authenticated))
    }

    @Test fun downRequiresTheKeyguardCurrentUserContextAndAllowedFacePolicy() {
        listOf(null, waiting.copy(showing = false), waiting.copy(handoffBlocked = true),
            waiting.copy(faceAllowed = false)).forEach { state ->
            val handoff = FaceTapNativeHandoff()
            handoff.pointerDown(1000L, state)
            assertFalse(handoff.pending)
        }
        val invalidTime = FaceTapNativeHandoff()
        invalidTime.pointerDown(0L, waiting)
        assertFalse(invalidTime.pending)
    }
}
