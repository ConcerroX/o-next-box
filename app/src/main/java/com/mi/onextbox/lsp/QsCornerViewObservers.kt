package com.mi.onextbox.lsp

import android.view.View
import android.view.ViewTreeObserver
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Observe only known QS views, including frames whose child RenderNodes are cached. */
internal class QsCornerViewObservers(key: String, private val onLayout: ((View) -> Unit)? = null, private val update: (View) -> Unit) {
    private val guards = WeakHashMap<View, Guard>()
    init {
        HotReloadLifecycle.register(key,
            detach = { synchronized(guards) { guards.values.toList() }.forEach { it.dispose() } },
            restore = { synchronized(guards) { guards.values.toList() }.forEach { it.attach() } })
        HotReloadLifecycle.registerReplay("$key:views",
            capture = { synchronized(guards) { guards.keys.toTypedArray() } },
            replay = { saved -> (saved as? Array<*>)?.filterIsInstance<View>()?.forEach(::watch) })
    }
    fun watch(view: View) {
        val guard = synchronized(guards) { guards.getOrPut(view) { Guard(view) } }
        guard.attach()
    }
    private inner class Guard(view: View) : View.OnAttachStateChangeListener, ViewTreeObserver.OnPreDrawListener, ViewTreeObserver.OnGlobalLayoutListener {
        private val target = WeakReference(view)
        private var tree: WeakReference<ViewTreeObserver>? = null
        private var listening = false
        fun attach() {
            val view = target.get() ?: return
            if (!listening) {
                listening = true
                view.addOnAttachStateChangeListener(this)
                if (view.isAttachedToWindow) arm(view)
                view.invalidate()
            }
        }
        private fun arm(view: View) {
            val next = view.viewTreeObserver
            if (tree?.get() === next && next.isAlive) return
            disarm()
            if (next.isAlive) {
                next.addOnPreDrawListener(this)
                if (onLayout != null) next.addOnGlobalLayoutListener(this)
                tree = WeakReference(next)
            }
        }
        private fun disarm() {
            tree?.get()?.takeIf { it.isAlive }?.let {
                it.removeOnPreDrawListener(this)
                if (onLayout != null) it.removeOnGlobalLayoutListener(this)
            }
            tree = null
        }
        fun dispose() {
            disarm()
            target.get()?.removeOnAttachStateChangeListener(this)
            listening = false
        }
        override fun onViewAttachedToWindow(view: View) { arm(view) }
        override fun onViewDetachedFromWindow(view: View) { disarm() }
        override fun onGlobalLayout() = HotReloadLifecycle.withHook(fallback = {}) {
            if (!HotReloadLifecycle.isRetiring) target.get()?.let { onLayout?.invoke(it) }
            Unit
        }
        override fun onPreDraw(): Boolean = HotReloadLifecycle.withHook(fallback = { true }) {
            if (!HotReloadLifecycle.isRetiring) target.get()?.let(update)
            true
        }
    }
}
