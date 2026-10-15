package com.mi.onextbox.ui.onboarding

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class OnboardingCompletionContractTest {
    private fun source() = listOf(File("src/main/java/com/mi/onextbox/ui/onboarding/OnboardingScreen.kt"),
        File("app/src/main/java/com/mi/onextbox/ui/onboarding/OnboardingScreen.kt")).first { it.isFile }.readText()

    @Test fun animationsKeepTheirExistingTransitionButActionsAreLockedUntilIdle() {
        val ui = source()
        assertTrue(ui.contains("pageTransition.AnimatedContent("))
        assertTrue(ui.contains("transitionSpec = { c17ActivationPageTransition() }"))
        assertTrue(ui.contains("!pageTransition.isRunning && pageTransition.currentState == activePage"))
        assertTrue(ui.contains("pageTransition.targetState == activePage) pageTransitioning = false"))
        assertTrue(ui.contains("OnboardingNavigationRules.acceptsAction("))
        assertFalse(ui.contains("agreementIndex += 1"))
    }

    @Test fun agreementActionsUseTheDisplayedPageAndPersistBeforeChangingUiState() {
        val ui = source()
        assertTrue(ui.contains("if (acceptsPageAction(page))"))
        assertTrue(ui.contains("onNext = pageAction(page)"))
        assertTrue(ui.contains("OnboardingNavigationRules.canContinueAgreements(snapshot.acceptedAgreements)"))
        assertTrue(ui.contains("OnboardingPreferences.setAllAgreementsAccepted("))
        val navigate = ui.substringAfter("fun navigateTo(").substringBefore("fun returnToMissingAgreement(")
        assertTrue(navigate.indexOf("if (!saved)") < navigate.indexOf("step = newStep"))
        assertTrue(navigate.indexOf("pageTransitioning = true") < navigate.indexOf("step = newStep"))
    }

    @Test fun completionHasEarlyAndCommitTimeConsentChecksWithSpecificRecovery() {
        val ui = source()
        assertTrue(ui.contains("DraftStep.Complete && requireCurrentAgreements()"))
        assertTrue(ui.contains("!pageTransitioning && Agreement.entries.all { agreementState[it] == true }"))
        val finish = ui.substringAfter("DraftStep.Complete -> CompletePage(").substringBefore("private fun AnimatedContentTransitionScope")
        assertTrue(finish.indexOf("requireCurrentAgreements()") < finish.indexOf("exitProgress.animateTo("))
        assertTrue(finish.contains("OnboardingPreferences.completeWithResult("))
        assertTrue(finish.contains("result == CompletionResult.AgreementsRequired"))
        assertTrue(finish.contains("returnToMissingAgreement(result)"))
        assertTrue(ui.contains("navigateTo(DraftStep.Agreements, R.string.onboarding_agreements_required)"))
        assertTrue(ui.contains("it in snapshot.acceptedAgreements"))
        assertFalse(ui.contains("saveFailed = !saved"))
    }

    @Test fun unifiedPageHasOneConfirmationAndAllDocumentsStartCollapsed() {
        val ui = source()
        val page = ui.substringAfter("private fun UnifiedAgreementsPage(")
            .substringBefore("private fun ExpandableAgreementCard(")
        assertTrue(page.contains("mutableStateOf<Agreement?>(null)"))
        assertTrue(page.contains("agreementRows.forEachIndexed"))
        assertEquals(1, "Checkbox(".toRegex(RegexOption.LITERAL).findAll(page).count())
        assertTrue(page.contains("R.string.onboarding_agreements_accept_all"))
        assertTrue(page.contains("enabled = accepted"))
        assertFalse(ui.contains("AgreementSequencePage"))
        assertFalse(ui.contains("page.agreementIndex"))
        val card = ui.substringAfter("private fun ExpandableAgreementCard(")
            .substringBefore("private fun RootAccessPage(")
        assertTrue(card.contains("AnimatedVisibility("))
        assertTrue(card.contains("expandVertically("))
        assertTrue(card.contains("shrinkVertically("))
        assertTrue(card.contains("context.readAgreementText(row.agreement)"))
    }

    @Test fun missingAgreementMessageExistsInEveryCompleteOnboardingLocale() {
        val root = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        val locales = listOf("values", "values-zh-rCN", "values-zh-rTW", "values-b+yue+Hant", "values-ja-rCN",
            "values-ja", "values-ko", "values-vi", "values-ru")
        for (locale in locales) {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File(root, "$locale/strings.xml")).getElementsByTagName("string")
            val messages = (0 until nodes.length).map { nodes.item(it) }
                .filter { it.attributes.getNamedItem("name").nodeValue == "onboarding_agreements_required" }
            assertEquals(locale, 1, messages.size)
            assertTrue(locale, messages.single().textContent.isNotBlank())
            val confirmation = (0 until nodes.length).map { nodes.item(it) }
                .filter { it.attributes.getNamedItem("name").nodeValue == "onboarding_agreements_accept_all" }
            assertEquals(locale, 1, confirmation.size)
            assertTrue(locale, confirmation.single().textContent.isNotBlank())
        }
    }
}
