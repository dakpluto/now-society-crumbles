package com.dakpluto.society.ui.map

import com.dakpluto.society.engine.terrain.TerrainGrid
import javafx.scene.canvas.Canvas

/**
 * Redrawn-per-cycle (not continuously animated) grid renderer, per CONCEPT.md
 * -> "Main map view" -> "already-decided redrawn-per-cycle ... JavaFX Canvas
 * rendering approach". [render] repaints the whole grid; occupancy/territory
 * overlays and disaster highlights are future layers on top of this base.
 */
class MapView(private val cellSizePx: Double = 12.0) : Canvas() {
    fun render(grid: TerrainGrid) {
        width = grid.widthCells * cellSizePx
        height = grid.heightCells * cellSizePx

        val gc = graphicsContext2D
        gc.clearRect(0.0, 0.0, width, height)
        for (y in 0 until grid.heightCells) {
            for (x in 0 until grid.widthCells) {
                gc.fill = TerrainColors.colorFor(grid[x, y])
                gc.fillRect(x * cellSizePx, y * cellSizePx, cellSizePx, cellSizePx)
            }
        }
    }
}
