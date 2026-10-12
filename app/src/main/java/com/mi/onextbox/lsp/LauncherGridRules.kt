package com.mi.onextbox.lsp

import kotlin.math.min

internal data class LauncherGrid(val columns: Int = 4, val rows: Int = 7)

internal data class LauncherGridState(val enabled: Boolean = false, val grid: LauncherGrid = LauncherGrid())

internal data class LauncherGridOption(val columns: Int, val textRows: Int, val wordlessRows: Int) {
    fun grid(wordless: Boolean) = LauncherGrid(columns, if (wordless) wordlessRows else textRows)
}

internal object LauncherGridRules {
    const val MIN_COLUMNS = 3
    const val MAX_COLUMNS = 6
    const val MIN_ROWS = 4
    const val MAX_ROWS = 10

    fun valid(grid: LauncherGrid) = grid.columns in MIN_COLUMNS..MAX_COLUMNS && grid.rows in MIN_ROWS..MAX_ROWS

    fun parse(value: String?): LauncherGridState {
        val parts = value?.split(':') ?: return LauncherGridState()
        if (parts.size != 3 || parts[0] !in listOf("0", "1")) return LauncherGridState()
        val grid = LauncherGrid(parts[1].toIntOrNull() ?: return LauncherGridState(),
            parts[2].toIntOrNull() ?: return LauncherGridState())
        return if (valid(grid)) LauncherGridState(parts[0] == "1", grid) else LauncherGridState()
    }

    fun encode(state: LauncherGridState): String {
        require(valid(state.grid))
        return "${if (state.enabled) 1 else 0}:${state.grid.columns}:${state.grid.rows}"
    }

    fun combinations(maximum: LauncherGrid): List<LauncherGridOption> {
        if (!valid(maximum)) return emptyList()
        return (min(4, maximum.columns)..maximum.columns).flatMap { columns ->
            (min(6, maximum.rows)..maximum.rows).map { rows -> LauncherGridOption(columns, rows, rows + 1) }
        }
    }

    fun expand(options: List<LauncherGridOption>, maximum: LauncherGrid): List<LauncherGridOption> {
        if (!valid(maximum)) return options
        val additions = (combinations(maximum) + LauncherGridOption(maximum.columns, maximum.rows, maximum.rows))
            .filter { it !in options }
        return if (additions.isEmpty()) options else options + additions
    }

    fun supports(maximum: LauncherGrid, columns: Int, rows: Int, wordless: Boolean): Boolean {
        if (columns == -1 && rows == -1) return false
        if (valid(maximum) && (columns == maximum.columns || columns == -1) &&
            (rows == maximum.rows || rows == -1)) return true
        return combinations(maximum).any {
            val grid = it.grid(wordless)
            (columns == -1 || columns == grid.columns) && (rows == -1 || rows == grid.rows)
        }
    }

    fun previewCellSize(width: Int, height: Int, paddingX: Int, paddingY: Int,
        grid: LauncherGrid, stockWidth: Int, stockHeight: Int): Int? {
        if (grid.columns !in MIN_COLUMNS..MAX_COLUMNS || grid.rows !in MIN_ROWS..MAX_ROWS + 1 ||
            width <= 0 || height <= 0 || stockWidth <= 0 || stockHeight <= 0) return null
        val availableWidth = width.toLong() - 2L * paddingX.coerceAtLeast(0)
        val availableHeight = height.toLong() - 2L * paddingY.coerceAtLeast(0)
        val fittingWidth = (availableWidth - (grid.columns - 1)) / grid.columns
        val fittingHeight = (availableHeight - (grid.rows - 1)) / grid.rows
        if (fittingWidth < 1 || fittingHeight < 1) return null
        return min(min(stockWidth, stockHeight).toLong(), min(fittingWidth, fittingHeight)).toInt()
    }
}
