package com.dakpluto.society.engine.terrain

/**
 * The four terrain element categories, each generated from its own independent
 * noise field. See CONCEPT.md -> "Terrain composition elements" and "Spatial
 * model: map grid and per-cell elements".
 */
enum class TerrainCategory {
    TOPOGRAPHY,
    GROUND_COMPOSITION,
    VEGETATION_COVER,
    WATER_FEATURES,
}

/**
 * The full terrain element list, grouped by [TerrainCategory]. See CONCEPT.md
 * -> "Terrain composition elements" for the source list; only the element list
 * is locked so far, per-archetype weight values are still deferred.
 */
enum class TerrainElement(val category: TerrainCategory) {
    // Topography (elevation/shape)
    FLAT_PLAINS(TerrainCategory.TOPOGRAPHY),
    HILLS(TerrainCategory.TOPOGRAPHY),
    MOUNTAINS(TerrainCategory.TOPOGRAPHY),
    VALLEY_CANYON(TerrainCategory.TOPOGRAPHY),
    PLATEAU(TerrainCategory.TOPOGRAPHY),

    // Ground composition (surface material)
    SAND(TerrainCategory.GROUND_COMPOSITION),
    CLAY(TerrainCategory.GROUND_COMPOSITION),
    LOAM(TerrainCategory.GROUND_COMPOSITION),
    ROCK(TerrainCategory.GROUND_COMPOSITION),
    PERMAFROST(TerrainCategory.GROUND_COMPOSITION),
    PEAT_BOG(TerrainCategory.GROUND_COMPOSITION),

    // Vegetation cover
    BARREN(TerrainCategory.VEGETATION_COVER),
    GRASS_SCRUBLAND(TerrainCategory.VEGETATION_COVER),
    FOREST_TREES(TerrainCategory.VEGETATION_COVER),
    MOSS_TUNDRA(TerrainCategory.VEGETATION_COVER),
    MARSH_MANGROVE(TerrainCategory.VEGETATION_COVER),

    // Water features (geographic adjacency)
    COASTLINE(TerrainCategory.WATER_FEATURES),
    RIVER_LAKE_ADJACENT(TerrainCategory.WATER_FEATURES),
    WETLAND_SATURATED(TerrainCategory.WATER_FEATURES),
    LANDLOCKED(TerrainCategory.WATER_FEATURES);

    companion object {
        private val byCategory: Map<TerrainCategory, List<TerrainElement>> =
            entries.groupBy { it.category }

        /** All elements belonging to [category], in declaration order. */
        fun of(category: TerrainCategory): List<TerrainElement> = byCategory.getValue(category)
    }
}
