package com.dakpluto.society.ui.map

import com.dakpluto.society.engine.terrain.Cell
import com.dakpluto.society.engine.terrain.TerrainCategory
import com.dakpluto.society.engine.terrain.TerrainElement
import javafx.scene.paint.Color

/**
 * Placeholder cell coloring for the map view: base color from the dominant
 * Ground composition element, tinted by dominant Vegetation cover, shaded by
 * dominant Topography, and overlaid for non-Landlocked Water features. See
 * CONCEPT.md -> "Main map view" -> "Cell rendering derives from terrain
 * element composition". Exact color/icon mapping is flagged there as an
 * art-direction pass, not architecture - this is a rough-and-ready stand-in
 * so the generated grid can be eyeballed for correctness.
 */
object TerrainColors {
    private val GROUND_COLORS = mapOf(
        TerrainElement.SAND to Color.web("#D8C08A"),
        TerrainElement.CLAY to Color.web("#B5713C"),
        TerrainElement.LOAM to Color.web("#8B5A2B"),
        TerrainElement.ROCK to Color.web("#8C8C8C"),
        TerrainElement.PERMAFROST to Color.web("#C9D6DF"),
        TerrainElement.PEAT_BOG to Color.web("#4B3621"),
    )

    private val VEGETATION_COLORS = mapOf(
        TerrainElement.GRASS_SCRUBLAND to Color.web("#9ACD32"),
        TerrainElement.FOREST_TREES to Color.web("#2E5E3B"),
        TerrainElement.MOSS_TUNDRA to Color.web("#A9C1A9"),
        TerrainElement.MARSH_MANGROVE to Color.web("#556B2F"),
    )
    private const val VEGETATION_BLEND = 0.35

    private val TOPOGRAPHY_BRIGHTNESS = mapOf(
        TerrainElement.FLAT_PLAINS to 1.0,
        TerrainElement.HILLS to 0.9,
        TerrainElement.MOUNTAINS to 0.7,
        TerrainElement.VALLEY_CANYON to 0.85,
        TerrainElement.PLATEAU to 1.05,
    )

    private val WATER_TINT = Color.web("#4A90D9")
    private const val WATER_BLEND = 0.25

    fun colorFor(cell: Cell): Color {
        val ground = GROUND_COLORS.getValue(cell.dominant(TerrainCategory.GROUND_COMPOSITION))

        val vegetation = cell.dominant(TerrainCategory.VEGETATION_COVER)
        var color = VEGETATION_COLORS[vegetation]?.let { ground.interpolate(it, VEGETATION_BLEND) } ?: ground

        val brightness = TOPOGRAPHY_BRIGHTNESS.getValue(cell.dominant(TerrainCategory.TOPOGRAPHY))
        color = color.deriveColor(0.0, 1.0, brightness, 1.0)

        if (cell.dominant(TerrainCategory.WATER_FEATURES) != TerrainElement.LANDLOCKED) {
            color = color.interpolate(WATER_TINT, WATER_BLEND)
        }

        return color
    }
}
