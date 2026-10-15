package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class LauncherWidgetSheetContractTest {
    private fun source(name: String) = listOf(File("src/main/java/com/mi/onextbox/lsp/$name.kt"),
        File("app/src/main/java/com/mi/onextbox/lsp/$name.kt")).first { it.isFile }.readText()

    @Test fun launcherStaticStateIsNotInitializedWhileInstallingHooks() {
        val adapter = source("LauncherWidgetSheetAdapter")
        val installation = adapter.substringBefore("private fun bindNativeCells(sheet: View)")
        assertFalse(installation.contains("getStaticIntField"))
        assertFalse(installation.contains("LauncherSettings"))
        val binding = adapter.substringAfter("private fun bindNativeCells(sheet: View)")
        assertTrue(binding.contains("CONTAINER_BOTTOM_WIDGETS_TRAY"))
        assertTrue(binding.contains("val sourceContainer = Reflect.getStaticIntField("))
    }

    @Test fun c17CellListenersAreOnTheNativeCellNotTheMissingLegacyPreviewChild() {
        val adapter = source("LauncherWidgetSheetAdapter")
        assertTrue(adapter.contains("widget_cell_oplus"))
        assertTrue(adapter.contains("cell.setOnClickListener(sheet as View.OnClickListener)"))
        assertTrue(adapter.contains("cell.setOnLongClickListener(sheet as View.OnLongClickListener)"))
        assertFalse(adapter.contains("\"widget_preview_container\""))
        assertTrue(adapter.contains("\"applyFromCellItem\", widget, 1f, preview, null"))
        assertTrue(adapter.contains("\"setSourceContainer\", sourceContainer"))
        assertTrue(adapter.contains("CONTAINER_BOTTOM_WIDGETS_TRAY"))
        assertTrue(adapter.contains("\"applyPreview\", bitmap"))
    }

    @Test fun packageUserFilteringAndTableGroupingStayNative() {
        val adapter = source("LauncherWidgetSheetAdapter")
        assertTrue(adapter.contains("\"getTargetComponent\""))
        assertTrue(adapter.contains("\"user\""))
        assertTrue(adapter.contains("Reflect.newInstance(packageKey"))
        assertTrue(adapter.contains("\"getWidgetsForPackageUser\", key"))
        assertTrue(adapter.contains("\"groupWidgetItemsIntoTableWithReordering\", span, items"))
        assertFalse(adapter.contains("getAllWidgets"))
        assertFalse(adapter.contains("bindAppWidgetId"))
        assertFalse(adapter.contains("allocateAppWidgetId"))
    }

    @Test fun onlyRestoredActionsAndTheirPanelsUseTheCompatibilityBinderEvenAfterTheSwitchChanges() {
        val hook = source("LauncherWidgetMenuHooker")
        val adapter = source("LauncherWidgetSheetAdapter")
        assertTrue(hook.contains("LauncherWidgetSheetAdapter(loader, ::report, ::diagnose).also { it.install() }"))
        assertTrue(hook.contains("sheets.recordAction(it)"))
        assertTrue(adapter.contains("if (!scope.ownsAction(chain.thisObject)) return@Hooker chain.proceed()"))
        assertTrue(adapter.contains("scope.duringAction"))
        assertTrue(adapter.contains("scope.recordOpeningSheet(sheet)"))
        assertTrue(adapter.contains("if (!scope.ownsSheet(sheet)) return@Hooker chain.proceed()"))
        assertFalse(adapter.contains("widgetsXposed()"))
        assertFalse(adapter.contains("View::class.java.getDeclaredMethod"))
        assertTrue(source("LauncherWidgetSheetScope").contains("WeakHashMap<Any, Boolean>()"))
    }

    @Test fun failedBindingCannotFallBackIntoTheCrashingNativeBinderOrLeaveOldPreviewJobsRunning() {
        val adapter = source("LauncherWidgetSheetAdapter")
        val bind = adapter.substringAfter("\"ONextBox-LauncherWidgets:cells\"").substringBefore("(listOf(action")
        assertTrue(bind.contains("runCatching { bindNativeCells(sheet) }"))
        assertTrue(bind.contains("closeFailedSheet(sheet)"))
        assertFalse(bind.substringAfter("runCatching { bindNativeCells(sheet) }").contains("chain.proceed()"))
        assertTrue(adapter.contains("scope.openingSheets().filterIsInstance<View>().forEach(::closeFailedSheet)"))
        assertTrue(adapter.contains("mLayoutChangeListenerToShowTips"))
        assertTrue(adapter.contains("mShowEducationTipTask"))
        assertTrue(adapter.contains("created.forEach(::clearCell)"))
        assertTrue(adapter.indexOf("Reflect.callMethod(cell, \"applyFromCellItem\"") < adapter.indexOf("table.removeAllViews()"))
        assertTrue(adapter.contains("clearRows(table)"))
        assertTrue(adapter.contains("sheet.post {"))
        assertTrue(adapter.contains("deoptimize(method)"))
    }
}
