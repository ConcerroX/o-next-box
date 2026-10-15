package com.mi.onextbox.lsp

import android.graphics.Bitmap
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.Toast
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernReflect as Reflect
import io.github.libxposed.api.XposedInterface
import java.util.function.Consumer

/** C17 retains WidgetsBottomSheet, but its widget_cell layout is now an OplusWidgetCell.
 * The old binder assumes a widget_preview_container child which that layout no longer has.
 * Bind the native cell itself, like OplusWidgetsHzAdapter does, without changing global layouts.
 */
internal class LauncherWidgetSheetAdapter(
    private val loader: ClassLoader,
    private val report: (String) -> Unit,
    private val diagnose: (String, Throwable) -> Unit,
) {
    private val scope = LauncherWidgetSheetScope()
    private val sheetType = Reflect.findClass("com.android.launcher3.widget.WidgetsBottomSheet", loader)
    private val cellType = Reflect.findClass("com.android.launcher3.widget.WidgetCell", loader)
    private val packageKey = Reflect.findClass("com.android.launcher3.util.PackageUserKey", loader)
    private val tableUtils = Reflect.findClass("com.android.launcher3.widget.util.WidgetsTableUtils", loader)

    fun recordAction(action: Any) = scope.recordAction(action)

    fun install() {
        val action = Reflect.findClass("com.android.launcher3.popup.SystemShortcut\$Widgets", loader)
            .getDeclaredMethod("onClick", View::class.java)
        val info = Reflect.findClass("com.android.launcher3.model.data.ItemInfo", loader)
        val populate = sheetType.getDeclaredMethod("populateAndShow", info)
        val bind = sheetType.getDeclaredMethod("onWidgetsBound")

        ModernHookRegistry.installFast("ONextBox-LauncherWidgets:click", action, XposedInterface.Hooker { chain ->
            if (!scope.ownsAction(chain.thisObject)) return@Hooker chain.proceed()
            scope.duringAction {
                runCatching { chain.proceed() }.onFailure { error ->
                    diagnose("Opening per-app widget sheet", error)
                    scope.openingSheets().filterIsInstance<View>().forEach(::closeFailedSheet)
                }.getOrNull()
            }
        })
        ModernHookRegistry.installFast("ONextBox-LauncherWidgets:sheet", populate, XposedInterface.Hooker { chain ->
            val sheet = chain.thisObject as? View ?: return@Hooker chain.proceed()
            if (scope.recordOpeningSheet(sheet)) {
                // The legacy education task assumes a nonempty old preview hierarchy.
                // Do not write its global preference or affect the normal widget drawer.
                runCatching {
                    (Reflect.getObjectField(sheet, "mLayoutChangeListenerToShowTips") as? View.OnLayoutChangeListener)
                        ?.let(sheet::removeOnLayoutChangeListener)
                    (Reflect.getObjectField(sheet, "mShowEducationTipTask") as? Runnable)?.let(sheet::removeCallbacks)
                }.onFailure { diagnose("Legacy widget education cleanup", it) }
            }
            chain.proceed()
        })
        ModernHookRegistry.installFast("ONextBox-LauncherWidgets:cells", bind, XposedInterface.Hooker { chain ->
            val sheet = chain.thisObject as? View ?: return@Hooker chain.proceed()
            if (!scope.ownsSheet(sheet)) return@Hooker chain.proceed()
            runCatching { bindNativeCells(sheet) }.onFailure { error ->
                diagnose("Binding per-app Oplus widget cells", error)
                closeFailedSheet(sheet)
            }
            // Never retry the incompatible old binder after a failure.
            null
        })
        (listOf(action, populate, bind) + sheetType.declaredMethods.filter { it.name == "onMeasure" }).forEach { method ->
            runCatching { ModernHookRuntime.requireModule().deoptimize(method) }
                .onFailure { diagnose("Widget sheet caller deoptimization: ${method.name}", it) }
        }
        report("Scoped Oplus widget-cell binding installed")
    }

    private fun bindNativeCells(sheet: View) {
        // Reading static fields initializes their declaring class. Favorites needs
        // launcher mode/context state, which is not ready during package loading.
        // Resolve UI constants only when a running launcher opens this sheet.
        val ids = Reflect.findClass("com.android.launcher3.R\$id", loader)
        val layouts = Reflect.findClass("com.android.launcher3.R\$layout", loader)
        val tableId = Reflect.getStaticIntField(ids, "widgets_table")
        val cellLayout = runCatching { Reflect.getStaticIntField(layouts, "widget_cell_oplus") }
            .getOrElse { Reflect.getStaticIntField(layouts, "widget_cell") }
        val sourceContainer = Reflect.getStaticIntField(
            Reflect.findClass("com.android.launcher3.LauncherSettings\$Favorites", loader), "CONTAINER_BOTTOM_WIDGETS_TRAY")
        val table = requireNotNull(sheet.findViewById<TableLayout>(tableId))
        val item = requireNotNull(Reflect.getObjectField(sheet, "mOriginalItemInfo"))
        val component = requireNotNull(Reflect.callMethod(item, "getTargetComponent"))
        val user = requireNotNull(Reflect.getObjectField(item, "user"))
        val activity = requireNotNull(Reflect.getObjectField(sheet, "mActivityContext"))
        val provider = requireNotNull(Reflect.callMethod(activity, "getPopupDataProvider"))
        val key = Reflect.newInstance(packageKey, Reflect.callMethod(component, "getPackageName"), user)
        val items = requireNotNull(Reflect.callMethod(provider, "getWidgetsForPackageUser", key) as? List<*>)
        val span = (Reflect.getObjectField(sheet, "mMaxHorizontalSpan") as Number).toInt()
        val groups = requireNotNull(Reflect.callStaticMethod(tableUtils,
            "groupWidgetItemsIntoTableWithReordering", span, items) as? List<*>)
        val created = mutableListOf<View>()
        val rows = try {
            groups.map { group ->
                TableRow(sheet.context).apply {
                    gravity = Gravity.TOP
                    for (widget in requireNotNull(group as? List<*>)) {
                        val cell = LayoutInflater.from(sheet.context).inflate(cellLayout, this, false)
                        check(cellType.isInstance(cell)) { "Unexpected native widget cell" }
                        created.add(cell)
                        // OplusWidgetCell intercepts child touches. Listeners must be on
                        // the cell, not a missing/hidden legacy preview child.
                        cell.setOnClickListener(sheet as View.OnClickListener)
                        cell.setOnLongClickListener(sheet as View.OnLongClickListener)
                        Reflect.callMethod(cell, "setAnimatePreview", false)
                        Reflect.callMethod(cell, "setSourceContainer", sourceContainer)
                        addView(cell)
                        val preview = Consumer<Bitmap?> { bitmap ->
                            runCatching { Reflect.callMethod(cell, "applyPreview", bitmap) }
                                .onFailure { diagnose("Native widget preview", it) }
                        }
                        Reflect.callMethod(cell, "applyFromCellItem", widget, 1f, preview, null)
                    }
                }
            }
        } catch (error: Throwable) {
            created.forEach(::clearCell)
            throw error
        }
        // Prepare everything before replacing the visible table; release old preview jobs.
        clearRows(table)
        table.removeAllViews()
        rows.forEach(table::addView)
        report("Per-app Oplus widget cells bound")
    }

    private fun clearCell(cell: View) {
        if (cellType.isInstance(cell)) runCatching { Reflect.callMethod(cell, "clear") }
    }
    private fun clearRows(parent: ViewGroup) {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (cellType.isInstance(child)) clearCell(child) else if (child is ViewGroup) clearRows(child)
        }
    }
    private fun closeFailedSheet(sheet: View) {
        // Posting also handles a failure during populateAndShow, before attachment.
        sheet.post {
            runCatching { Reflect.callMethod(sheet, "close", false) }
                .onFailure {
                    runCatching { (sheet.parent as? ViewGroup)?.removeView(sheet) }
                }
            runCatching {
                val strings = Reflect.findClass("com.android.launcher3.R\$string", loader)
                Toast.makeText(sheet.context, Reflect.getStaticIntField(strings, "activity_not_available"), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
