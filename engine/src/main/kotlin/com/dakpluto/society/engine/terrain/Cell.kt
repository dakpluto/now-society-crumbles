package com.dakpluto.society.engine.terrain

/**
 * One map cell's local terrain element composition - a weight per
 * [TerrainElement], with the weights within each [TerrainCategory] summing to
 * 1.0. See CONCEPT.md -> "Spatial model: map grid and per-cell elements".
 */
data class Cell(val elementWeights: Map<TerrainElement, Double>) {
    fun weightOf(element: TerrainElement): Double = elementWeights[element] ?: 0.0

    /** The most heavily weighted element within [category] for this cell. */
    fun dominant(category: TerrainCategory): TerrainElement =
        TerrainElement.of(category).maxBy { weightOf(it) }
}
