package com.mi.onextbox.lsp

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.widget.TextView
import com.mi.onextbox.R
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicBoolean

/** Artwork-only replacement. Subscription, visibility, roaming and radio state remain native. */
internal object LargeNetworkArtworkHooker {
    internal fun supportsModel(name: String) = name ==
        "com.android.systemui.statusbar.pipeline.mobile.domain.model.OplusNetworkTypeTextModel\$DefaultNetworkTypeText"
    internal fun label(value: String?, lte: Boolean): String? {
        val key = when (value?.uppercase()) {
            "G" -> "g"; "E" -> "e"; "H" -> "h"; "H+" -> "h_plus"
            "2G" -> "2g"; "2G+" -> "2g_plus"; "3G" -> "3g"; "3G+" -> "3g_plus"
            "4G" -> if (lte) "lte" else "4g"
            "4G+" -> if (lte) "lte_plus" else "4g_plus"
            "5G" -> "5g"; "5G+" -> "5g_plus"; "5GA", "5G-A" -> "5g_a"
            "LTE" -> "lte"; "LTE+" -> "lte_plus"
            "1X" -> "1x"; "4.5G" -> "4_5g"; "4.5G+" -> "4_5g_plus"
            "5.5G" -> "5_5g"; "5G++" -> "5g_plus_plus"
            "5GE" -> "5g_e"; "5GUC", "5G UC" -> "5g_uc"
            "5GUW", "5G UW", "5GUWB" -> "5g_uw"
            "TIGO_5G_CONNECTED" -> "tigo_connected"
            "TIGO_5G_NOT_RESTRICTED" -> "tigo_available"
            else -> null
        }
        return key
    }

    private val icons = mapOf(
        "1x" to R.drawable.onext_network_1x,
        "4_5g" to R.drawable.onext_network_4_5g, "4_5g_plus" to R.drawable.onext_network_4_5g_plus,
        "5_5g" to R.drawable.onext_network_5_5g, "5g_plus_plus" to R.drawable.onext_network_5g_plus_plus,
        "5g_e" to R.drawable.onext_network_5g_e, "5g_uc" to R.drawable.onext_network_5g_uc,
        "5g_uw" to R.drawable.onext_network_5g_uw,
        "tigo_connected" to R.drawable.onext_network_tigo_connected,
        "tigo_available" to R.drawable.onext_network_tigo_available,
        "g" to R.drawable.onext_network_g, "e" to R.drawable.onext_network_e,
        "h" to R.drawable.onext_network_h, "h_plus" to R.drawable.onext_network_h_plus,
        "2g" to R.drawable.onext_network_2g, "2g_plus" to R.drawable.onext_network_2g_plus,
        "3g_plus" to R.drawable.onext_network_3g_plus,
        "3g" to R.drawable.onext_network_3g, "4g" to R.drawable.onext_network_4g,
        "4g_plus" to R.drawable.onext_network_4g_plus, "5g" to R.drawable.onext_network_5g,
        "5g_plus" to R.drawable.onext_network_5g_plus, "5g_a" to R.drawable.onext_network_5g_a,
        "lte" to R.drawable.onext_network_lte, "lte_plus" to R.drawable.onext_network_lte_plus,
    )
    private val resourceLabels = mapOf(
        "ic_1x_mobiledata_updated" to "1X",
        "ic_3g_mobiledata_updated" to "3G", "stat_signal_connected_3gp_lte_big" to "3G+",
        "ic_5g_mobiledata_updated" to "5G", "ic_5g_plus_mobiledata_updated" to "5G+",
        "stat_sys_data_fully_connected_5_dot_5g" to "5.5G",
        "stat_signal_connected_4_5g_lte_big" to "4.5G",
        "stat_signal_connected_4_5g_plus" to "4.5G+",
        "ic_5g_e_mobiledata_updated" to "5GE", "stat_sys_data_fully_connected_5g_e" to "5GE",
        "stat_sys_data_fully_connected_5g_plus_plus" to "5G++",
        "stat_sys_data_fully_connected_5g_uc" to "5GUC",
        "stat_sys_data_fully_connected_5g_uwb_global" to "5GUW", "ic_5g_uwb_mobiledata" to "5GUW",
        "stat_cust_tigo_5g_connect" to "TIGO_5G_CONNECTED",
        "stat_cust_tigo_5g_connect_legacy" to "TIGO_5G_CONNECTED",
        "stat_cust_tigo_5g_full" to "TIGO_5G_NOT_RESTRICTED",
        "stat_cust_tigo_5g_full_legacy" to "TIGO_5G_NOT_RESTRICTED",
        "stat_signal_connected_2g_lte_big" to "2G",
        "stat_signal_connected_g_lte_big" to "G", "stat_signal_connected_e_lte_big" to "E",
        "stat_signal_connected_h_lte_big" to "H", "stat_signal_connected_h_p_lte_big" to "H+",
        "stat_signal_connected_3g_lte_big" to "3G", "stat_signal_connected_4g_lte_big" to "4G",
        "stat_signal_connected_big_4g" to "4G", "stat_signal_connected_4gp_lte_big" to "4G+",
        "stat_signal_connected_big_4g_plus" to "4G+", "stat_signal_connected_5g" to "5G",
        "stat_signal_connected_big_5g" to "5G", "stat_sys_data_fully_connected_5g_plus" to "5G+",
        "stat_sys_data_fully_connected_5g_a" to "5G-A", "stat_signal_connected_lte_big" to "LTE",
        "stat_signal_connected_lte_plus_big" to "LTE+",
    ) + mapOf("g" to "G", "e" to "E", "h" to "H", "h_plus" to "H+", "3g" to "3G",
        "2g" to "2G", "2g_plus" to "2G+", "3g_plus" to "3G+",
        "4g" to "4G", "4g_plus" to "4G+", "5g" to "5G", "5g_plus" to "5G+",
        "5g_plus_mobiledata_default" to "5G+", "5g_a" to "5G-A", "lte" to "LTE", "lte_plus" to "LTE+")
        .mapKeys { (key, _) -> if (key.endsWith("_default")) "ic_$key" else "ic_${key}_mobiledata" }

    private val delivered = AtomicBoolean()
    private val failed = AtomicBoolean()
    private fun report() {
        if (delivered.compareAndSet(false, true)) HookLog.i("ONextBox-LargeNetwork", "Custom network artwork rendered")
    }
    private fun failure(t: Throwable) {
        if (failed.compareAndSet(false, true)) HookLog.w("ONextBox-LargeNetwork", "Artwork unavailable; using native renderer", t)
    }
    private fun drawable(context: Context, key: String): Drawable {
        val module = context.createPackageContext("com.mi.onextbox", 0)
        return requireNotNull(module.getDrawable(icons.getValue(key))).mutate()
    }
    private fun modelLabel(model: Any?): String? {
        model ?: return null
        // Resolve carrier identities before prefix/suffix, since both Tigo states say "5G".
        // The ROM strips InnerClass annotations, so getSimpleName() may include '$'.
        if (!supportsModel(model.javaClass.name)) return null
        val text = model.javaClass.getMethod("getNetworkTypeText").invoke(model) ?: return null
        val name = text.javaClass.getMethod("getName").invoke(text) as? String
        if (name == "TIGO_5G_CONNECTED" || name == "TIGO_5G_NOT_RESTRICTED") return name
        val prefix = text.javaClass.getMethod("getPrefix").invoke(text) as? String ?: return null
        val suffix = text.javaClass.getMethod("getSuffix").invoke(text) as? String ?: return null
        return prefix + suffix
    }

    fun hook(loader: ClassLoader) {
        runCatching { hookResources() }.onFailure(::failure)
        runCatching { hookView(loader) }.onFailure(::failure)
        runCatching { hookCompose(loader) }.onFailure(::failure)
        runCatching { hookCompose(loader, true) }.onFailure(::failure)
    }

    private fun hookResources() {
        val method = Resources::class.java.getDeclaredMethod("getDrawableForDensity", Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType, Resources.Theme::class.java)
        ModernHookRegistry.installFast("large-network-artwork:resources", method, XposedInterface.Hooker { chain ->
            if (!LargeNetworkTypeSettings.usesArtworkXposed()) return@Hooker chain.proceed()
            val replacement = runCatching {
                val res = chain.thisObject as Resources
                val id = chain.getArg(0) as Int
                if (id == 0) return@runCatching null
                if (res.getResourcePackageName(id) != "com.android.systemui") return@runCatching null
                val name = resourceLabels[res.getResourceEntryName(id)] ?: return@runCatching null
                val key = label(name, LargeNetworkTypeSettings.usesLteXposed()) ?: return@runCatching null
                val context = Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as? Context
                    ?: return@runCatching null
                val module = context.createPackageContext("com.mi.onextbox", 0)
                module.resources.getDrawableForDensity(icons.getValue(key), chain.getArg(1) as Int, module.theme)?.mutate()
            }.getOrElse { failure(it); null }
            replacement?.also { report() } ?: chain.proceed()
        })
    }

    private fun hookView(loader: ClassLoader) {
        val binding = Class.forName("com.oplus.systemui.statusbar.pipeline.mobile.ui.view.OplusStatusBarMobileViewBinder\$Os17Binding", false, loader)
        val field = binding.getDeclaredField("mobileTypeText").apply { isAccessible = true }
        val bind = binding.declaredMethods.single { it.name == "bindNetworkTypeText" && it.parameterCount == 1 }
        ModernHookRegistry.installFast("large-network-artwork:view", bind, XposedInterface.Hooker { chain ->
            val result = chain.proceed()
            if (LargeNetworkTypeSettings.usesArtworkXposed()) runCatching {
                val name = modelLabel(chain.getArg(0)) ?: return@runCatching
                val key = label(name, LargeNetworkTypeSettings.usesLteXposed()) ?: return@runCatching
                val view = field.get(chain.thisObject) as TextView
                if (key.startsWith("tigo_")) {
                    val imageField = binding.getDeclaredField("mobileTypeImage").apply { isAccessible = true }
                    val image = imageField.get(chain.thisObject) as android.widget.ImageView
                    image.setImageDrawable(drawable(image.context, key))
                    report()
                    return@runCatching
                }
                val text = SpannableString(view.text)
                if (text.isEmpty()) return@runCatching
                text.getSpans(0, text.length, ArtworkSpan::class.java).forEach(text::removeSpan)
                val icon = drawable(view.context, key)
                text.setSpan(ArtworkSpan(icon), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                view.text = text
                report()
            }.onFailure(::failure)
            result
        })
    }

    private class ArtworkSpan(private val icon: Drawable) : ReplacementSpan() {
        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
            // Preserve the native line box; keep the artwork's original 16dp scale.
            if (fm != null) paint.getFontMetricsInt(fm)
            return icon.intrinsicWidth
        }
        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float,
            top: Int, y: Int, bottom: Int, paint: Paint) {
            icon.setTint(paint.color)
            icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            val save = canvas.save()
            try {
                canvas.translate(x, (top + bottom - icon.intrinsicHeight) / 2f)
                icon.draw(canvas)
            } finally { canvas.restoreToCount(save) }
        }
    }

    private fun hookCompose(loader: ClassLoader, tigo: Boolean = false) {
        fun type(name: String) = Class.forName(name, false, loader)
        val ownerName = if (tigo) "TigoFiveGStackedMobileIconLayoutStrategyKt" else "OplusStackedMobileIconLayoutStrategyKt"
        val prefix = if (tigo) "TigoFiveGNetworkTypeText-" else "NetworkTypeText-"
        val owner = type("com.android.systemui.statusbar.pipeline.shared.ui.composable.$ownerName")
        val method = owner.declaredMethods.single { it.name.startsWith(prefix) && it.parameterCount == 13 }
        val bitmapType = type("androidx.compose.ui.graphics.ImageBitmap")
        val imageConstructor = type("androidx.compose.ui.graphics.AndroidImageBitmap").getConstructor(Bitmap::class.java)
        val iconMethod = type("androidx.compose.material3.IconKt").declaredMethods.single {
            it.name.startsWith("Icon-") && it.parameterTypes.firstOrNull() == bitmapType
        }
        val modifierType = type("androidx.compose.ui.Modifier")
        val defaultModifier = modifierType.getField("Companion").get(null)
        // Cache only immutable raster artwork, never views/activities or tinted shared drawables.
        val bitmaps = android.util.LruCache<String, Any>(24)
        val entered = AtomicBoolean()
        ModernHookRegistry.installFast("large-network-artwork:compose:$ownerName", method, XposedInterface.Hooker { chain ->
            if (!LargeNetworkTypeSettings.usesArtworkXposed()) return@Hooker chain.proceed()
            if (entered.compareAndSet(false, true)) HookLog.i("ONextBox-LargeNetwork",
                "Compose artwork entry: model=${chain.getArg(1)?.javaClass?.name}, LTE=${LargeNetworkTypeSettings.usesLteXposed()}")
            val prepared = runCatching {
                val name = modelLabel(chain.getArg(1)) ?: return@runCatching null
                val key = label(name, LargeNetworkTypeSettings.usesLteXposed()) ?: return@runCatching null
                val vm = chain.getArg(0) ?: return@runCatching null
                val context = vm.javaClass.getMethod("getMobileContext").invoke(vm) as? Context
                    ?: Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as? Context
                    ?: return@runCatching null
                val cacheKey = "$key:${context.resources.displayMetrics.densityDpi}"
                val image = synchronized(bitmaps) {
                    bitmaps.get(cacheKey) ?: run {
                        val icon = drawable(context, key)
                        val bitmap = Bitmap.createBitmap(icon.intrinsicWidth.coerceAtLeast(1), icon.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                        icon.setBounds(0, 0, bitmap.width, bitmap.height)
                        icon.draw(Canvas(bitmap))
                        imageConstructor.newInstance(bitmap).also { bitmaps.put(cacheKey, it) }
                    }
                }
                val model = chain.getArg(1)!!
                val descriptionId = model.javaClass.getMethod("getContentDescription").invoke(model) as Int
                image to if (descriptionId != 0) context.getString(descriptionId) else name
            }.getOrElse { failure(it); null } ?: return@Hooker chain.proceed()
            // All lookup/rendering can fail back before entering Compose. Never fall back
            // after a composable starts: that would corrupt the host's group stack.
            try {
                iconMethod.invoke(null, prepared.first, prepared.second, chain.getArg(3) ?: defaultModifier,
                    chain.getArg(2), chain.getArg(10), 0, 0)
                report()
                null
            } catch (error: InvocationTargetException) { throw error.targetException }
        })
        HookLog.i("ONextBox-LargeNetwork", "Artwork View/resource/Compose hooks installed")
    }
}
