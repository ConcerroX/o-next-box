package com.mi.onextbox.ui.onboarding

import com.mi.onextbox.ui.onboarding.OnboardingPreferences.Agreement
import com.mi.onextbox.ui.onboarding.OnboardingPreferences.DraftStep

/** Outgoing AnimatedContent pages cannot navigate or change consent for the current page. */
internal object OnboardingNavigationRules {
    fun acceptsAction(
        sourceStep: DraftStep, currentStep: DraftStep,
        transitioning: Boolean, exiting: Boolean,
    ): Boolean = !transitioning && !exiting && sourceStep == currentStep

    fun canContinueAgreements(accepted: Set<Agreement>): Boolean = accepted.containsAll(Agreement.entries)
}
