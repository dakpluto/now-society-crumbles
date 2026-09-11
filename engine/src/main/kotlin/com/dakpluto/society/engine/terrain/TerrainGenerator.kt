package com.dakpluto.society.engine.terrain

import com.dakpluto.society.engine.config.MapConfig
import com.dakpluto.society.engine.random.SimRandom
import kotlin.math.floor

/**
 * Procedurally generates a [TerrainGrid] from a run seed: one Perlin noise
 * field per [TerrainCategory], each seeded deterministically off the run's own
 * seed via [SimRandom], so the same config always produces the identical map.
 * See CONCEPT.md -> "Spatial model: map grid and per-cell elements" ->
 * "Generation algorithm: noise-based (Perlin/simplex)".
 *
 * Placeholder pass: each category's noise value is blended across its
 * elements in fixed declaration order, uniformly spaced - it does not yet
 * bias toward an archetype's target element-composition weights, since those
 * weights are themselves still deferred in CONCEPT.md (see "Weight-sourcing
 * methodology"). Once they're defined, the blend step below is where that
 * biasing plugs in.
 */
object TerrainGenerator {
    private const val DEFAULT_FREQUENCY = 0.08

    fun generate(seed: Long, map: MapConfig, frequency: Double = DEFAULT_FREQUENCY): TerrainGrid {
        val seedRandom = SimRandom(seed)
        val fieldsByCategory = TerrainCategory.entries.associateWith {
            NoiseField2D(seed = seedRandom.nextLong(), frequency = frequency)
        }
        val elementsByCategory = TerrainCategory.entries.associateWith { TerrainElement.of(it) }

        val cells = (0 until map.heightCells).flatMap { y ->
            (0 until map.widthCells).map { x ->
                val weights = TerrainCategory.entries.fold(emptyMap<TerrainElement, Double>()) { acc, category ->
                    val noiseValue = fieldsByCategory.getValue(category).sample(x.toDouble(), y.toDouble())
                    acc + blend(elementsByCategory.getValue(category), noiseValue)
                }
                Cell(weights)
            }
        }

        return TerrainGrid(map.widthCells, map.heightCells, cells)
    }

    /**
     * Spreads a single noise value across [elements] (declaration order),
     * blending between the two nearest elements so adjacent cells shift
     * smoothly from one to the next rather than snapping abruptly.
     */
    private fun blend(elements: List<TerrainElement>, noiseValue: Double): Map<TerrainElement, Double> {
        val n = elements.size
        if (n == 1) return mapOf(elements[0] to 1.0)

        val virtualIndex = noiseValue.coerceIn(0.0, 1.0) * (n - 1)
        val lower = floor(virtualIndex).toInt().coerceIn(0, n - 1)
        val upper = (lower + 1).coerceAtMost(n - 1)
        val frac = virtualIndex - lower

        if (lower == upper) return mapOf(elements[lower] to 1.0)

        return mapOf(
            elements[lower] to (1.0 - frac),
            elements[upper] to frac,
        )
    }
}
