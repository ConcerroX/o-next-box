package com.mi.onextbox.lsp

import android.content.Context
import android.graphics.Rect
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernHookRuntime
import com.mi.onextbox.lsp.compat.ModernMethodHook
import com.mi.onextbox.lsp.compat.ModernReflect
import java.util.concurrent.atomic.AtomicBoolean

internal object StatusBarLayoutHooker {
    private const val TAG = "ONextBox-StatusBarLayout"
    fun hook(loader: ClassLoader) {
        runCatching { hookOrder(loader) }.onFailure { HookLog.w(TAG, "Icon-order adapter unavailable", it) }
        runCatching { hookMargins(loader) }.onFailure { HookLog.w(TAG, "Margin adapter unavailable", it) }
    }

    private fun hookOrder(loader: ClassLoader) {
        val type = ModernReflect.findClass("com.android.systemui.statusbar.phone.ui.StatusBarIconList", loader)
        val slots = type.getDeclaredField("mSlots").apply { isAccessible = true }
        val slot = ModernReflect.findClass("${type.name}\$Slot", loader)
        val name = slot.getMethod("getName")
        val constructor = type.getDeclaredConstructor(Array<String>::class.java)
        val reached = AtomicBoolean()
        ModernHookRegistry.installCompat("$TAG:order", constructor, object : ModernMethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.throwable != null || !StatusBarLayoutSettings.orderEnabledXposed()) return
                // Run after construction so explicit custom order wins over the legacy C16 preset.
                val list = slots.get(param.thisObject) as? MutableList<*> ?: return
                val names = list.map { name.invoke(it) as String }
                val sortedNames = StatusBarLayoutRules.reorder(names, StatusBarLayoutSettings.orderXposed())
                val buckets = list.groupBy { name.invoke(it) as String }.mapValues { it.value.toMutableList() }
                val sorted = sortedNames.map { buckets.getValue(it).removeAt(0) }
                @Suppress("UNCHECKED_CAST")
                (list as MutableList<Any?>).apply { clear(); addAll(sorted) }
                if (reached.compareAndSet(false, true)) HookLog.i(TAG, "Native status-bar slots reordered")
            }
        })
        HookLog.i(TAG, "Native icon-order hook installed")
    }

    private fun hookMargins(loader: ClassLoader) {
        val type = ModernReflect.findClass("com.oplus.systemui.statusbar.phone.StatusBarPaddingProvider", loader)
        val pair = ModernReflect.findClass("kotlin.Pair", loader)
        val method = type.getDeclaredMethod("getHomeStatusBarLeftRightPadding", String::class.java, pair, pair)
        val context = type.getDeclaredField("context").apply { isAccessible = true }
        val first = pair.getMethod("getFirst")
        val second = pair.getMethod("getSecond")
        val reached = AtomicBoolean()
        ModernHookRegistry.installCompat("$TAG:margins", method, object : ModernMethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.throwable != null) return
                val offsets = StatusBarLayoutSettings.marginsXposed() ?: return
                val original = param.result as? Rect ?: return
                val density = (context.get(param.thisObject) as Context).resources.displayMetrics.density
                val cutouts = param.args[2] ?: return
                val leftSafe = (first.invoke(cutouts) as Number).toInt()
                val rightSafe = (second.invoke(cutouts) as Number).toInt()
                // Return a copy, keep native top/bottom and cutout avoidance, and never
                // accumulate offsets on the View or change lock-screen padding.
                param.result = Rect(StatusBarLayoutRules.padding(original.left, offsets.first, density, leftSafe), original.top,
                    StatusBarLayoutRules.padding(original.right, offsets.second, density, rightSafe), original.bottom)
                if (reached.compareAndSet(false, true)) HookLog.i(TAG, "Native home status-bar margins adjusted")
            }
        })
        ModernReflect.findClassIfExists("com.oplus.systemui.statusbar.phone.PhoneStatusBarViewExImpl", loader)
            ?.declaredMethods?.filter { it.name == "updateContentsPadding" }?.forEach {
                runCatching { ModernHookRuntime.requireModule().deoptimize(it) }
                    .onFailure { error -> HookLog.w(TAG, "Padding caller deoptimization unavailable", error) }
            }
        HookLog.i(TAG, "Native side-margin hook installed")
    }
}
