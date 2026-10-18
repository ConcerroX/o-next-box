package com.mi.onextbox.lsp

import android.view.View
import android.widget.TextView
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect
import io.github.libxposed.api.XposedInterface

internal object LauncherIconLabelSpacingHooker {
    private const val TAG = "ONextBox-LauncherIconLabelSpacing"
    private const val BUBBLE_TEXT_VIEW = "com.android.launcher3.BubbleTextView"
    private const val MULTIPLE_SIZE_GROUP_VIEW = "com.android.launcher3.stack.view.IMultipleSizeGroupView"
    private const val CARD_NAME_HELPER = "com.android.launcher3.card.utils.CardNameHelper"

    // Workspace(0), all-apps drawer(1), folder(2) and the drawer's category folder(11) share the
    // same icon/label layout path. Hot-seat(8) and super power workspace(9) are excluded.
    private val targetDisplays = setOf(0, 1, 2, 11)

    fun hook(loader: ClassLoader) {
        runCatching { installIcons(loader) }.onSuccess {
            HookLog.i(TAG, "Icon spacing gate installed")
        }.onFailure {
            HookLog.w(TAG, "Icon spacing unavailable, stock spacing retained", it)
        }

        runCatching { installFolderName(loader) }.onSuccess {
            HookLog.i(TAG, "Folder label spacing gate installed")
        }.onFailure {
            HookLog.w(TAG, "Folder label spacing unavailable, stock spacing retained", it)
        }

        runCatching { installCardName(loader) }.onSuccess {
            HookLog.i(TAG, "Card label spacing gate installed")
        }.onFailure {
            HookLog.w(TAG, "Card label spacing unavailable, stock spacing retained", it)
        }
    }

    // Normal and large app icons are BubbleTextViews whose icon/label gap is the compound drawable
    // padding. Hook the getter updateIconBounds() reads on every setIcon().
    private fun installIcons(loader: ClassLoader) {
        val bubble = ModernReflect.findClass(BUBBLE_TEXT_VIEW, loader)
        val getter = bubble.declaredMethods.single {
            it.name == "getRegularCompoundDrawablePadding"
                    && it.parameterTypes.isEmpty()
                    && it.returnType == Int::class.javaPrimitiveType
        }
        check(ModernHookRuntime.requireModule().deoptimize(getter)) {
            "Unable to deoptimize icon-label padding getter"
        }
        // Deoptimizing only the getter cannot invalidate an inlined copy in its caller.
        bubble.declaredMethods.filter { it.name == "updateIconBounds" }.forEach {
            check(ModernHookRuntime.requireModule().deoptimize(it)) {
                "Unable to deoptimize icon bounds update"
            }
        }

        ModernHookRegistry.installFast("$TAG:padding", getter, XposedInterface.Hooker { chain ->
            val original = chain.proceed() as? Int ?: 0
            val offset = LauncherGestureSettings.iconLabelSpacingOffsetXposed()
            if (offset == 0 || !LauncherGestureSettings.iconLabelSpacingEnabledXposed()) return@Hooker original
            val host = chain.thisObject as? View ?: return@Hooker original
            val display = runCatching {
                val extInstance =
                    ModernReflect.getObjectField(host, "mOPlusBtvExtV2") ?: return@runCatching -1
                ModernReflect.callMethod(extInstance, "getIconDisplay") as? Int ?: -1
            }.getOrDefault(-1)
            if (display !in targetDisplays) return@Hooker original
            val delta = (offset * host.resources.displayMetrics.density + 0.5f).toInt()
            // Each layout owns its native base; never borrow another view/profile's padding.
            (original + delta).coerceAtLeast(0)
        })
    }

    // A big folder's label is a bottom-aligned BubbleTextView whose compound drawable padding is
    // hard-coded to 0; its icon/label gap is the label's top padding (folder_icon_title_padding_top).
    // The real layout lives in IMultipleSizeGroupView.placeGroupName(Launcher, DeviceProfile, String);
    // FlexibleFolderIcon only forwards to it, so hook the interface default method directly.
    private fun installFolderName(loader: ClassLoader) {
        val groupView = ModernReflect.findClass(MULTIPLE_SIZE_GROUP_VIEW, loader)
        val placeGroupName = groupView.declaredMethods.single {
            it.name == "placeGroupName"
                    && it.parameterTypes.size == 3
                    && it.returnType == Boolean::class.javaPrimitiveType
        }
        runCatching { ModernHookRuntime.requireModule().deoptimize(placeGroupName) }
        ModernHookRegistry.installCompat(
            "$TAG:folder-name", placeGroupName, object : ModernMethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // Landscape and unsupported layouts may return without resetting padding.
                    if (param.throwable != null || param.result != true) return
                    val offset = LauncherGestureSettings.iconLabelSpacingOffsetXposed()
                    if (offset == 0 || !LauncherGestureSettings.iconLabelSpacingEnabledXposed()) return
                    val host = param.thisObject as? View ?: return
                    val label = runCatching { ModernReflect.callMethod(host, "getTitle") as? View }
                        .getOrNull() ?: return
                    shiftLabelDown(label, offset)
                }
            })
    }

    // A service card's name label uses the same top-padding-based gap, set in CardNameHelper's
    // measureCardName()/measureGroupCardName(). Shift it identically.
    private fun installCardName(loader: ClassLoader) {
        val helper = ModernReflect.findClass(CARD_NAME_HELPER, loader)
        listOf("measureCardName", "measureGroupCardName").forEach { name ->
            val measure =
                helper.declaredMethods.single { it.name == name && it.parameterTypes.size == 1 && !it.isBridge }
            check(ModernHookRuntime.requireModule().deoptimize(measure)) {
                "Unable to deoptimize card name measurement"
            }
            ModernHookRegistry.installCompat(
                "$TAG:card-name:$name", measure, object : ModernMethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.throwable != null) return
                        val offset = LauncherGestureSettings.iconLabelSpacingOffsetXposed()
                        if (offset == 0 || !LauncherGestureSettings.iconLabelSpacingEnabledXposed()) return
                        val host = param.thisObject ?: return
                        val label = runCatching {
                            ModernReflect.callMethod(host, "getCardName") as? View
                        }.getOrNull() ?: return
                        // Group-card measurement does nothing without layout parameters.
                        if (label.layoutParams == null) return
                        shiftLabelDown(label, offset)
                    }
                })
        }
    }

    private fun shiftLabelDown(label: View, offset: Int) {
        val delta = (offset * label.resources.displayMetrics.density + 0.5f).toInt()
        if (delta == 0) return
        // The label is bottom-aligned with a fixed height, so its text top is
        // containerBottom - height + paddingTop. Increasing only paddingTop moves the text down by
        // delta; changing height too would cancel the shift.
        val text = label as? TextView ?: return
        val height = label.layoutParams?.height ?: return
        val metrics = text.paint.fontMetricsInt
        val textHeight = if (text.includeFontPadding) metrics.bottom - metrics.top else metrics.descent - metrics.ascent
        val top = LauncherLabelSpacingRules.labelTop(label.paddingTop, delta, height, label.paddingBottom, textHeight)
        if (top == label.paddingTop) return
        label.setPadding(
            label.paddingLeft,
            top,
            label.paddingRight,
            label.paddingBottom,
        )
        label.requestLayout()
    }
}

internal object LauncherLabelSpacingRules {
    // Native folder/card labels have a fixed height. Do not move their only line out of bounds.
    fun labelTop(base: Int, delta: Int, height: Int, bottom: Int, textHeight: Int): Int {
        if (height <= 0 || delta <= 0) return base
        val available = (height - bottom - textHeight - base).coerceAtLeast(0)
        return base + delta.coerceAtMost(available)
    }
}
