package com.mi.onextbox.lsp

import java.util.UUID

internal data class LauncherGridApplyRequest(val token: String, val issuedAt: Long, val grid: LauncherGrid) {
    fun encode() = "$token|$issuedAt|${grid.columns}|${grid.rows}"
    fun isCurrent(now: Long, state: LauncherGridState) =
        state.enabled && state.grid == grid && now >= issuedAt && now - issuedAt < LIFETIME

    companion object {
        const val LIFETIME = 120_000L

        fun create(grid: LauncherGrid, now: Long): LauncherGridApplyRequest {
            require(LauncherGridRules.valid(grid) && now >= 0)
            return LauncherGridApplyRequest(UUID.randomUUID().toString(), now, grid)
        }

        fun parse(value: String?): LauncherGridApplyRequest? {
            val parts = value?.split('|') ?: return null
            if (parts.size != 4) return null
            val token = runCatching { UUID.fromString(parts[0]).toString() }.getOrNull() ?: return null
            if (token != parts[0]) return null
            val issuedAt = parts[1].toLongOrNull()?.takeIf { it >= 0 } ?: return null
            val grid = LauncherGrid(parts[2].toIntOrNull() ?: return null, parts[3].toIntOrNull() ?: return null)
            return if (LauncherGridRules.valid(grid)) LauncherGridApplyRequest(token, issuedAt, grid) else null
        }
    }
}

internal class LauncherGridApplyQueue {
    var pending: LauncherGridApplyRequest? = null
        private set
    var running: LauncherGridApplyRequest? = null
        private set
    var handled: String? = null
        private set

    fun offer(request: LauncherGridApplyRequest?) {
        pending = request?.takeIf { it.token != handled && it.token != running?.token }
    }

    fun consume(): LauncherGridApplyRequest? = pending?.also {
        handled = it.token
        pending = null
    }

    fun start(): LauncherGridApplyRequest? {
        if (running != null) return null
        return consume()?.also { running = it }
    }

    fun finish() { running = null }

    fun capture(): Array<String?> = arrayOf(pending?.encode(), handled)

    fun restore(value: Any?) {
        val values = value as? Array<*>
        pending = LauncherGridApplyRequest.parse(values?.getOrNull(0) as? String)
        handled = values?.getOrNull(1) as? String
        running = null
    }
}
