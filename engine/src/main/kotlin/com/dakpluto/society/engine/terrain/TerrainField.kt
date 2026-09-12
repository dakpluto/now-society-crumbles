package com.dakpluto.society.engine.terrain

import com.dakpluto.society.engine.random.SimRandom
import kotlin.math.floor

/**
 * The continuous procedural terrain field a run's seed defines: one Perlin
 * noise field per [TerrainCategory], each seeded deterministically off the
 * run's own seed via [SimRandom], so the same seed always produces the
 * identical field. See CONCEPT.md -> "Spatial model: map grid and per-cell
 * elements" -> "Generation algorithm: noise-based (Perlin/simplex)".
 *
 * This is deliberately continuous, not grid-snapped - [elementWeightsAt]
 * accepts any (x, y), not just integer cell coordinates. [TerrainGenerator]
 * samples it at integer coordinates to build the discrete [TerrainGrid] the
 * simulation reads; a renderer wanting smoother-than-one-cell transitions
 * (no hard edge where a cell's blend snaps to its neighbor's) can instead
 * sample this directly at sub-cell resolution, since the underlying noise
 * varies smoothly everywhere regardless of cell boundaries.
 *
 * Placeholder pass: each category's noise value is blended across its
 * elements in fixed declaration order, uniformly spaced - it does not yet
 * bias toward an archetype's target element-composition weights, since those
 * weights are themselves still deferred in CONCEPT.md (see "Weight-sourcing
 * methodology"). Once they're defined, the blend step below is where that
 * biasing plugs in.
 */
class TerrainField(seed: Long, frequency: Double) {
    private val fieldsByCategory = run {
        val seedRandom = SimRandom(seed)
        TerrainCategory.entries.associateWith { NoiseField2D(seed = seedRandom.nextLong(), frequency = frequency) }
    }
    private val elementsByCategory = TerrainCategory.entries.associateWith { TerrainElement.of(it) }

    /** [category]'s element weights at (x, y) - sums to 1.0. */
    fun categoryWeightsAt(category: TerrainCategory, x: Double, y: Double): Map<TerrainElement, Double> {
        val noiseValue = fieldsByCategory.getValue(category).sample(x, y)
        return blend(elementsByCategory.getValue(category), noiseValue)
    }

    /** Element weights at (x, y) across every category, merged - see [Cell.elementWeights]. */
    fun elementWeightsAt(x: Double, y: Double): Map<TerrainElement, Double> =
        TerrainCategory.entries.fold(emptyMap()) { acc, category -> acc + categoryWeightsAt(category, x, y) }

    /**
     * Spreads a single noise value across [elements] (declaration order),
     * blending between the two nearest elements so nearby points shift
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
