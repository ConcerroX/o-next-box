package com.mi.onextbox.errors

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class AppErrorHookContractTest {
    @Test fun observesOnlyTheInnermostOverloadAndAlwaysProceeds() {
        val source = source("lsp/AppErrorTrackingHooker.kt")
        assertTrue(source.contains("maxOfOrNull { it.parameterCount }"))
        assertTrue(source.contains("candidates.single { it.parameterCount == size }"))
        assertTrue(source.contains("HookConfigSnapshot.boolean(AppErrorTrackingSettings.KEY, false) == true"))
        assertTrue(source.contains("chain.proceed()"))
        assertFalse(source.contains("setResult"))
    }

    @Test fun hotReloadCannotDetachBusyWritesAndOwnsTheExecutor() {
        val source = source("lsp/AppErrorTrackingHooker.kt")
        assertTrue(source.contains("HotReloadLifecycle.register(\"errors:writer\""))
        assertTrue(source.contains("canReload = { pending.get() == 0 }"))
        assertTrue(source.contains("it.shutdown()"))
        assertTrue(source.contains("it.awaitTermination(250, TimeUnit.MILLISECONDS)"))
        assertTrue(source.contains("restore = { synchronized(workerLock) { worker = null } }"))
        assertTrue(source.contains("if (HotReloadLifecycle.isRetiring) return"))
    }

    @Test fun bridgeAndFileAccessRemainPrivateAndBounded() {
        val source = source("errors/AppErrorProvider.kt")
        assertTrue(source.contains("AppErrorRules.canWrite(Binder.getCallingUid())"))
        assertTrue(source.contains("if (!AppErrorTrackingSettings.isEnabled(ctx)) return Bundle.EMPTY"))
        assertTrue(source.contains("AppErrorRules.bounded(input, Process.myUid() / 100_000)"))
        assertTrue(source.contains("AppErrorRules.canRead(Binder.getCallingUid(), Process.myUid())"))
        assertTrue(source("lsp/AppErrorTrackingHooker.kt").contains("file.path.startsWith(\"/data/anr/\")"))
    }

    @Test fun newImplementationDoesNotContainCodeComments() {
        listOf("lsp/AppErrorTrackingHooker.kt", "errors/AppErrorModels.kt", "errors/AppErrorMetadata.kt", "errors/AppErrorDatabase.kt",
            "errors/AppErrorProvider.kt", "errors/AppErrorRepository.kt", "errors/AppErrorTrackingSettings.kt",
            "ui/screens/AppErrorTrackingPage.kt").forEach { path ->
            val source = source(path)
            assertFalse(path, source.contains("/*"))
            assertFalse(path, source.lineSequence().any { it.trimStart().startsWith("//") })
        }
    }

    @Test fun trackingPageOmitsInstructionHintsButKeepsCountsAndSearchSummary() {
        val page = source("ui/screens/AppErrorTrackingPage.kt")
        assertFalse(page.contains("R.string.app_errors_privacy"))
        assertFalse(page.contains("R.string.app_errors_loading_hint"))
        assertTrue(page.contains("R.string.app_errors_counts"))
        assertTrue(source("ui/screens/FeatureSearchIndex.kt").contains(
            "Entry(FeaturePageMode.AppErrors, R.string.app_errors_title, R.string.app_errors_enable_summary)",
        ))
    }

    @Test fun detailsUseTheSharedNestedPageTransitionAndExplicitDividers() {
        val root = source("ui/Root.kt")
        assertTrue(root.contains("data class AppErrorDetail(val id: Long) : RootRoute"))
        assertTrue(root.contains("subclass(RootRoute.AppErrorDetail::class)"))
        assertTrue(root.contains("navigator.push(RootRoute.AppErrorDetail(id))"))
        assertTrue(root.contains("entry<RootRoute.AppErrorDetail>"))
        val page = source("ui/screens/AppErrorTrackingPage.kt")
        assertFalse(page.contains("AnimatedContent"))
        assertFalse(page.contains("selectedId"))
        assertTrue(page.contains("if (index < rows.lastIndex) SettingsDivider()"))
        assertTrue(page.contains("onOpenDetail(record.id)"))
        assertTrue(page.contains("AppErrorInfoGroup(eventRows)"))
        assertTrue(page.contains("AppErrorInfoGroup(appRows)"))
        assertTrue(page.contains("AppErrorInfoGroup(environmentRows)"))
    }

    @Test fun summaryShowsThePackageBesideTheKindWithoutADuplicatePackageRow() {
        val page = source("ui/screens/AppErrorTrackingPage.kt")
        val card = page.substringAfter("internal fun AppErrorSummaryCard").substringBefore("private fun AppErrorDetailPage")
        assertFalse(card.contains("count: Int"))
        assertFalse(card.contains("app_errors_occurrences"))
        assertEquals(1, Regex("ErrorText\\(record\\.packageName,").findAll(card).count())
        assertTrue(card.contains("LogLevelBadge(errorKindLabel(record.kind), accent)\n                ErrorText(record.packageName, Modifier.weight(1f), secondary = true, maxLines = 1)"))
        assertTrue(card.contains("if (record.processName != record.packageName)"))
        assertTrue(page.contains("add(stringResource(R.string.app_errors_package) to value.packageName)"))
    }

    @Test fun metadataIsMigratedWithoutDeletingHistoryAndNotLoadedInLists() {
        val database = source("errors/AppErrorDatabase.kt")
        assertTrue(database.contains("ALTER TABLE errors ADD COLUMN metadata"))
        assertFalse(database.contains("DROP TABLE"))
        assertTrue(database.contains("metadata = if (withDetail)"))
        val hook = source("lsp/AppErrorTrackingHooker.kt")
        listOf("crash?.throwFileName", "crash?.throwClassName", "crash?.throwMethodName", "crash?.throwLineNumber", "Build.FINGERPRINT", "/data/tombstones/")
            .forEach { assertTrue(it, hook.contains(it)) }
        val provider = source("errors/AppErrorProvider.kt")
        assertTrue(provider.contains("putString(\"metadata\", AppErrorMetadataCodec.encode(metadata))"))
        assertTrue(provider.contains("metadata = AppErrorMetadataCodec.decode"))
    }

    private fun source(path: String): String = listOf(File("src/main/java/com/mi/onextbox/$path"), File("app/src/main/java/com/mi/onextbox/$path"))
        .first { it.isFile }.readText().replace("\r\n", "\n")
}
