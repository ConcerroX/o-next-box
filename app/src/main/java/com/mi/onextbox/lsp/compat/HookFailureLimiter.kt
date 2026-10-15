package com.mi.onextbox.lsp.compat

/** Bounded, process-local error accounting; never changes whether a callback executes. */
internal class HookFailureLimiter<K>(
    private val intervalMillis: Long = 60_000,
    private val capacity: Int = 256,
) {
    private data class Window(var emittedAt: Long, var suppressed: Long = 0)
    private val windows = LinkedHashMap<K, Window>()

    init { require(intervalMillis > 0 && capacity > 0) }

    /** null suppresses this log; otherwise returns repeats suppressed since the previous log. */
    @Synchronized
    fun record(key: K, nowMillis: Long): Long? {
        val window = windows[key]
        if (window == null) {
            if (windows.size >= capacity) windows.remove(windows.keys.first())
            windows[key] = Window(nowMillis)
            return 0
        }
        if (nowMillis >= window.emittedAt && nowMillis - window.emittedAt < intervalMillis) {
            if (window.suppressed < Long.MAX_VALUE) window.suppressed++
            return null
        }
        val suppressed = window.suppressed
        window.emittedAt = nowMillis
        window.suppressed = 0
        return suppressed
    }
}
