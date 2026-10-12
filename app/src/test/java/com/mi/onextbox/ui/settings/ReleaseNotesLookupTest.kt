package com.mi.onextbox.ui.settings

import org.junit.Assert.*
import org.junit.Test

class ReleaseNotesLookupTest {
    private fun release(tag: String, name: String = "", assets: List<String> = emptyList(), draft: Boolean = false) =
        ReleaseNotesEntry(tag, name, assets, "Changes", draft)

    @Test fun stableVersionAlwaysUsesMainRepositoryAndNeedsNoApk() {
        val found = ReleaseNotesLookup.find("17.0.0.57", { repo, _ ->
            assertEquals(AppUpdateRules.MAIN_REPOSITORY, repo)
            release("v17.0.0.57")
        }, { _, _ -> error("Unexpected fallback") })
        assertEquals("Changes", found?.body)
    }

    @Test fun betaVersionUsesBetaRepositoryEvenWithoutEnrollmentInput() {
        val found = ReleaseNotesLookup.find("17.0.0.58_Beta", { repo, _ ->
            assertEquals(AppUpdateRules.BETA_REPOSITORY, repo)
            release("V17.0.0.58_Beta")
        }, { _, _ -> error("Unexpected fallback") })
        assertNotNull(found)
    }

    @Test fun missingExactTagFallsBackToTitleOrAssetVersionWithoutBuildFiltering() {
        for (entry in listOf(release("test-58", "17.0.0.58_Beta"),
            release("test-58", assets = listOf("ONextBox-17.0.0.58_Beta-debug.apk")))) {
            val found = ReleaseNotesLookup.find("17.0.0.58_Beta", { _, _ -> null }, { repo, page ->
                assertEquals(AppUpdateRules.BETA_REPOSITORY, repo)
                assertEquals(1, page)
                listOf(entry)
            })
            assertEquals(entry, found)
        }
    }

    @Test fun wrongVersionDraftOrDifferentChannelCannotMatch() {
        assertFalse(release("17.0.0.59_Beta").matches("17.0.0.58_Beta"))
        assertFalse(release("17.0.0.58_Beta", draft = true).matches("17.0.0.58_Beta"))
        assertFalse(release("17.0.0.58_Beta").matches("17.0.0.58"))
        assertFalse(release("17.0.0.58").matches("17.0.0.58_Beta"))
        assertTrue(release("17.0.0.58", "17.0.0.58_Beta").matches("17.0.0.58_Beta"))
    }

    @Test fun fallbackContinuesAcrossFullPagesButStopsAtShortPage() {
        val pages = mutableListOf<Int>()
        val result = ReleaseNotesLookup.find("17.0.0.58_Beta", { _, _ -> null }, { _, page ->
            pages += page
            if (page == 1) List(100) { release("17.0.0.59_Beta") }
            else listOf(release("17.0.0.58_Beta"))
        })
        assertNotNull(result)
        assertEquals(listOf(1, 2), pages)
        assertNull(ReleaseNotesLookup.find("17.0.0.58", { _, _ -> null }, { _, page ->
            assertEquals(1, page)
            emptyList()
        }))
    }

    @Test fun cancellationIsNotTreatedAsMissingRelease() {
        val cancellation = java.util.concurrent.CancellationException()
        try {
            ReleaseNotesLookup.find("17.0.0.58", { _, _ -> throw cancellation }, { _, _ -> emptyList() })
            fail("Cancellation was swallowed")
        } catch (error: java.util.concurrent.CancellationException) {
            assertSame(cancellation, error)
        }
    }
}
