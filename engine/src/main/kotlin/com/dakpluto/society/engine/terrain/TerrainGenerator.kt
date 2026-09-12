package com.dakpluto.society.engine.terrain

import com.dakpluto.society.engine.config.MapConfig

/**
 * Materializes a discrete [TerrainGrid] (what the simulation reads) from a
 * [TerrainField] (the continuous procedural field a seed defines) by
 * sampling it at each integer cell coordinate. See [TerrainField] for the
 * actual generation algorithm.
 */
object TerrainGenerator {
    private const val DEFAULT_FREQUENCY = 0.08

    /** The continuous field itself, e.g. for a renderer that wants sub-cell-resolution sampling. */
    fun generateField(seed: Long, frequency: Double = DEFAULT_FREQUENCY): TerrainField = TerrainField(seed, frequency)

    fun generate(seed: Long, map: MapConfig, frequency: Double = DEFAULT_FREQUENCY): TerrainGrid {
        val field = generateField(seed, frequency)

        val cells = (0 until map.heightCells).flatMap { y ->
            (0 until map.widthCells).map { x -> Cell(field.elementWeightsAt(x.toDouble(), y.toDouble())) }
        }

        return TerrainGrid(map.widthCells, map.heightCells, cells)
    }
}
