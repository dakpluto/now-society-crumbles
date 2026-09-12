package com.dakpluto.society.ui.map

import com.dakpluto.society.engine.terrain.TerrainCategory
import com.dakpluto.society.engine.terrain.TerrainElement
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import javafx.scene.paint.Color

/**
 * Per-[TerrainElement] ARGB tile lookup for continuous terrain rendering (see
 * [TerrainRenderer]). Real art loads from `/textures/<element name>.png`
 * (lowercase enum name, e.g. `TerrainElement.FOREST_TREES` -> `forest_trees.png`)
 * when the resource exists. An element without art yet falls back to a flat
 * solid-color tile using the same placeholder colors the map used before
 * texture blending existed, so adding real art is a drop-in replacement one
 * element at a time - see ASSETS.md.
 *
 * Every tile is normalized to [TILE_SIZE] regardless of the source image's
 * own resolution, so real and fallback tiles blend on equal footing and
 * mismatched art sizes never need special-casing at render time. Topography
 * has no tiles at all - CONCEPT.md renders it as a brightness shift, not
 * art; see [TOPOGRAPHY_BRIGHTNESS].
 */
object TerrainTextures {
    const val TILE_SIZE = 256

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
    private const val VEGETATION_OVERLAY_ALPHA = 0.35

    private val WATER_COLOR = Color.web("#4A90D9")
    private const val WATER_OVERLAY_ALPHA = 0.25

    /** Weighted-brightness multiplier per Topography element - no art needed, see ASSETS.md. */
    val TOPOGRAPHY_BRIGHTNESS = mapOf(
        TerrainElement.FLAT_PLAINS to 1.0,
        TerrainElement.HILLS to 0.9,
        TerrainElement.MOUNTAINS to 0.7,
        TerrainElement.VALLEY_CANYON to 0.85,
        TerrainElement.PLATEAU to 1.05,
    )

    private val TRANSPARENT_TILE = IntArray(TILE_SIZE * TILE_SIZE)

    private val tiles: Map<TerrainElement, IntArray> = TerrainElement.entries
        .filterNot { it.category == TerrainCategory.TOPOGRAPHY }
        .associateWith(::loadTile)

    /** [element]'s ARGB tile, [TILE_SIZE] x [TILE_SIZE], row-major. Never called for Topography elements. */
    fun tileFor(element: TerrainElement): IntArray = tiles.getValue(element)

    private fun loadTile(element: TerrainElement): IntArray = when (element) {
        // The "no overlay" member of Vegetation cover / Water features - shows whatever is under it untouched.
        TerrainElement.BARREN, TerrainElement.LANDLOCKED -> TRANSPARENT_TILE
        else -> loadResourceTile(element) ?: fallbackTile(element)
    }

    private fun loadResourceTile(element: TerrainElement): IntArray? {
        val stream = javaClass.getResourceAsStream("/textures/${element.name.lowercase()}.png") ?: return null
        val image = Image(stream, TILE_SIZE.toDouble(), TILE_SIZE.toDouble(), false, true)
        val buffer = IntArray(TILE_SIZE * TILE_SIZE)
        image.pixelReader.getPixels(0, 0, TILE_SIZE, TILE_SIZE, PixelFormat.getIntArgbInstance(), buffer, 0, TILE_SIZE)
        return buffer
    }

    private fun fallbackTile(element: TerrainElement): IntArray {
        val (color, alpha) = when (element) {
            in GROUND_COLORS -> GROUND_COLORS.getValue(element) to 1.0
            in VEGETATION_COLORS -> VEGETATION_COLORS.getValue(element) to VEGETATION_OVERLAY_ALPHA
            else -> WATER_COLOR to WATER_OVERLAY_ALPHA
        }
        val argb = argbOf(color, alpha)
        return IntArray(TILE_SIZE * TILE_SIZE) { argb }
    }

    private fun argbOf(color: Color, alpha: Double): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
