package com.dakpluto.society.engine.terrain

import com.dakpluto.society.engine.config.MapConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TerrainGeneratorTest : StringSpec({
    val map = MapConfig(widthCells = 16, heightCells = 12)

    "the same seed produces an identical grid" {
        val a = TerrainGenerator.generate(seed = 42L, map = map)
        val b = TerrainGenerator.generate(seed = 42L, map = map)

        for (y in 0 until map.heightCells) {
            for (x in 0 until map.widthCells) {
                a[x, y] shouldBe b[x, y]
            }
        }
    }

    "different seeds produce different grids" {
        val a = TerrainGenerator.generate(seed = 1L, map = map)
        val b = TerrainGenerator.generate(seed = 2L, map = map)

        val cellsA = (0 until map.heightCells).flatMap { y -> (0 until map.widthCells).map { x -> a[x, y] } }
        val cellsB = (0 until map.heightCells).flatMap { y -> (0 until map.widthCells).map { x -> b[x, y] } }

        cellsA shouldNotBe cellsB
    }

    "every cell's per-category weights sum to 1.0" {
        val grid = TerrainGenerator.generate(seed = 5L, map = map)

        for (y in 0 until map.heightCells) {
            for (x in 0 until map.widthCells) {
                val cell = grid[x, y]
                for (category in TerrainCategory.entries) {
                    val total = TerrainElement.of(category).sumOf { cell.weightOf(it) }
                    total shouldBe (1.0 plusOrMinus 1e-9)
                }
            }
        }
    }

    "the grid dimensions match the requested map size" {
        val grid = TerrainGenerator.generate(seed = 3L, map = map)

        grid.widthCells shouldBe map.widthCells
        grid.heightCells shouldBe map.heightCells
    }
})
