package com.dakpluto.society.ui.map

import com.dakpluto.society.engine.config.MapConfig
import com.dakpluto.society.engine.terrain.TerrainGenerator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TerrainColorsTest : StringSpec({
    val grid = TerrainGenerator.generate(seed = 42L, map = MapConfig(widthCells = 16, heightCells = 12))

    "colorFor() is opaque and deterministic for every generated cell" {
        for (y in 0 until grid.heightCells) {
            for (x in 0 until grid.widthCells) {
                val cell = grid[x, y]
                val a = TerrainColors.colorFor(cell)
                val b = TerrainColors.colorFor(cell)

                a shouldBe b
                a.opacity shouldBe 1.0
            }
        }
    }
})
