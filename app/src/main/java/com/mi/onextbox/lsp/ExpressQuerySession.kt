package com.mi.onextbox.lsp

// Automatic submission and temporary keyboard state for parcel tracking.
internal class ExpressQuerySession {
    data class Pending(val number: String, val generation: Long, val deadline: Long)

    @Volatile var inExpress: Boolean = false
        private set
    @Volatile private var pending: Pending? = null
    @Volatile private var suppressImeUntil = 0L
    private var generation = 0L

    @Synchronized fun exportReloadState(): Array<Any?> = arrayOf(
        inExpress, pending?.let { arrayOf<Any?>(it.number, it.generation, it.deadline) }, suppressImeUntil, generation,
    )

    @Synchronized fun importReloadState(state: Any?) {
        val values = state as? Array<*> ?: return
        inExpress = values.getOrNull(0) as? Boolean ?: false
        val saved = values.getOrNull(1) as? Array<*>
        pending = if (saved?.size == 3) Pending(saved[0] as String, saved[1] as Long, saved[2] as Long) else null
        suppressImeUntil = values.getOrNull(2) as? Long ?: 0L
        generation = values.getOrNull(3) as? Long ?: 0L
    }

    @Synchronized fun receive(data: String?, now: Long) {
        inExpress = ExpressCardRules.isExpressUri(data)
        val number = ExpressCardRules.pendingTrackingNumber(data)
        generation++
        pending = number?.let { Pending(it, generation, now + 6_000L) }
        suppressImeUntil = if (number != null) now + 2_000L else 0L
    }

    fun pending(now: Long): Pending? = pending?.takeIf { now < it.deadline && inExpress }

    fun matches(query: Pending, now: Long): Boolean = pending(now)?.generation == query.generation

    @Synchronized fun suppressIme(query: Pending, now: Long) {
        if (matches(query, now)) suppressImeUntil = minOf(now + 2_000L, query.deadline)
    }

    @Synchronized fun complete(query: Pending, now: Long) {
        if (!matches(query, now)) return
        pending = null
        suppressImeUntil = now + 20L
    }

    fun imeSuppressed(now: Long): Boolean = inExpress && now < suppressImeUntil
}
