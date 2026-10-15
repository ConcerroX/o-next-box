package com.mi.onextbox.ui.onboarding

import com.mi.onextbox.ui.onboarding.OnboardingPreferences.Agreement
import com.mi.onextbox.ui.onboarding.OnboardingPreferences.DraftStep
import org.junit.Assert.*
import org.junit.Test

class OnboardingNavigationRulesTest {
    @Test fun duplicateOutgoingPageCallbackCannotSkipTheRootPage() {
        var step = DraftStep.Agreements
        var transitioning = false
        var writes = 0
        val oldPageClick = {
            if (OnboardingNavigationRules.acceptsAction(DraftStep.Agreements, step,
                    transitioning, false)) {
                writes++
                transitioning = true
                step = DraftStep.RootAccess
            }
        }
        oldPageClick()
        oldPageClick()
        assertEquals(DraftStep.RootAccess, step)
        assertEquals(1, writes)
        transitioning = false
        oldPageClick()
        assertEquals(DraftStep.RootAccess, step)
        assertEquals(1, writes)
    }

    @Test fun staleNonAgreementPagesCannotMoveTheCurrentFlowBackOrForward() {
        for (step in DraftStep.entries) {
            assertTrue(OnboardingNavigationRules.acceptsAction(step, step, false, false))
            for (other in DraftStep.entries.filter { it != step }) {
                assertFalse(OnboardingNavigationRules.acceptsAction(step, other, false, false))
            }
        }
    }

    @Test fun transitionsAndCompletionAnimationRejectAllPageActions() {
        for (step in DraftStep.entries) {
            assertFalse(OnboardingNavigationRules.acceptsAction(step, step, true, false))
            assertFalse(OnboardingNavigationRules.acceptsAction(step, step, false, true))
        }
    }

    @Test fun oldPartialConsentCannotEnableTheUnifiedConfirmation() {
        assertFalse(OnboardingNavigationRules.canContinueAgreements(emptySet()))
        assertFalse(OnboardingNavigationRules.canContinueAgreements(setOf(Agreement.TermsAndRisk)))
    }

    @Test fun unifiedPageCannotExitToRootUnlessAllSixAreAccepted() {
        assertFalse(OnboardingNavigationRules.canContinueAgreements(setOf(Agreement.DeviceAuthorization)))
        assertTrue(OnboardingNavigationRules.canContinueAgreements(Agreement.entries.toSet()))
        for (missing in Agreement.entries) {
            assertFalse(OnboardingNavigationRules.canContinueAgreements(Agreement.entries.toSet() - missing))
        }
    }
}
