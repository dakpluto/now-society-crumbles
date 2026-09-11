package com.dakpluto.society.engine.terrain

/**
 * The procedurally generated map: a [widthCells] x [heightCells] grid of
 * [Cell]s, row-major. See CONCEPT.md -> "Spatial model: map grid and per-cell
 * elements".
 */
class TerrainGrid(val widthCells: Int, val heightCells: Int, private val cells: List<Cell>) {
    init {
        require(cells.size == widthCells * heightCells) {
            "expected ${widthCells * heightCells} cells for a ${widthCells}x$heightCells grid, got ${cells.size}"
        }
    }

    operator fun get(x: Int, y: Int): Cell {
        require(x in 0 until widthCells && y in 0 until heightCells) {
            "($x, $y) is out of bounds for a ${widthCells}x$heightCells grid"
        }
        return cells[y * widthCells + x]
    }
}
