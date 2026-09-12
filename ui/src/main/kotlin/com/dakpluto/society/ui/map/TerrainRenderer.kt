package com.dakpluto.society.ui.map

import com.dakpluto.society.engine.terrain.TerrainCategory
import com.dakpluto.society.engine.terrain.TerrainField
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage

/**
 * Continuous, per-pixel terrain rendering: samples [TerrainField] at render
 * resolution (not once per cell) so the blend ratio between adjacent
 * elements changes smoothly across the whole map, with no hard edge at cell
 * boundaries - see CONCEPT.md -> "Cell rendering derives from terrain
 * element composition" and [TerrainField]'s own doc comment (validated
 * against a discrete, one-sample-per-cell comparison before this became the
 * live renderer).
 *
 * Composites all four terrain categories in the order CONCEPT.md describes:
 * an opaque Ground composition base, a Vegetation cover overlay, Topography
 * as a brightness multiplier (no art - see ASSETS.md), then a Water
 * features overlay. Real vs. not-yet-supplied art is transparent to this
 * renderer - see [TerrainTextures].
 *
 * Works on raw ARGB int arrays throughout (bulk-read tiles, blend into a
 * plain IntArray, bulk-write it to the output image once) rather than
 * per-pixel `Color` objects - `PixelReader.getColor`/`PixelWriter.setColor`
 * allocate a `Color` per call and are far too slow for a few hundred
 * thousand pixels.
 */
object TerrainRenderer {
    fun render(
        field: TerrainField,
        widthCells: Int,
        heightCells: Int,
        pixelsPerCell: Double,
        cellsPerTextureTile: Double,
    ): WritableImage {
        val widthPx = (widthCells * pixelsPerCell).toInt()
        val heightPx = (heightCells * pixelsPerCell).toInt()
        val tileSize = TerrainTextures.TILE_SIZE
        val texelsPerWorldUnit = tileSize / cellsPerTextureTile

        val outputArgb = IntArray(widthPx * heightPx)
        for (py in 0 until heightPx) {
            val worldY = py / pixelsPerCell
            val texelY = wrapTexel(worldY * texelsPerWorldUnit, tileSize)
            val rowOffset = py * widthPx

            for (px in 0 until widthPx) {
                val worldX = px / pixelsPerCell
                val texelX = wrapTexel(worldX * texelsPerWorldUnit, tileSize)

                outputArgb[rowOffset + px] = compositeAt(field, worldX, worldY, texelY * tileSize + texelX)
            }
        }

        val output = WritableImage(widthPx, heightPx)
        output.pixelWriter.setPixels(0, 0, widthPx, heightPx, PixelFormat.getIntArgbInstance(), outputArgb, 0, widthPx)
        return output
    }

    private fun compositeAt(field: TerrainField, worldX: Double, worldY: Double, texelIndex: Int): Int {
        var result = blendCategory(field, TerrainCategory.GROUND_COMPOSITION, worldX, worldY, texelIndex)
        result = alphaOver(blendCategory(field, TerrainCategory.VEGETATION_COVER, worldX, worldY, texelIndex), result)
        result = applyBrightness(result, topographyBrightnessAt(field, worldX, worldY))
        result = alphaOver(blendCategory(field, TerrainCategory.WATER_FEATURES, worldX, worldY, texelIndex), result)
        return result
    }

    /** Weighted sum of every nonzero-weight element's texel in [category] - at most two, per [TerrainField]. */
    private fun blendCategory(
        field: TerrainField,
        category: TerrainCategory,
        worldX: Double,
        worldY: Double,
        texelIndex: Int,
    ): Int {
        var a = 0.0
        var r = 0.0
        var g = 0.0
        var b = 0.0
        for ((element, weight) in field.categoryWeightsAt(category, worldX, worldY)) {
            if (weight <= 0.0) continue
            val argb = TerrainTextures.tileFor(element)[texelIndex]
            a += weight * ((argb ushr 24) and 0xFF)
            r += weight * ((argb ushr 16) and 0xFF)
            g += weight * ((argb ushr 8) and 0xFF)
            b += weight * (argb and 0xFF)
        }
        return (a.toInt() shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    private fun topographyBrightnessAt(field: TerrainField, worldX: Double, worldY: Double): Double =
        field.categoryWeightsAt(TerrainCategory.TOPOGRAPHY, worldX, worldY)
            .entries.sumOf { (element, weight) -> weight * TerrainTextures.TOPOGRAPHY_BRIGHTNESS.getValue(element) }

    private fun applyBrightness(argb: Int, brightness: Double): Int {
        val a = (argb ushr 24) and 0xFF
        val r = (((argb ushr 16) and 0xFF) * brightness).toInt().coerceIn(0, 255)
        val g = (((argb ushr 8) and 0xFF) * brightness).toInt().coerceIn(0, 255)
        val b = ((argb and 0xFF) * brightness).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Standard "over" alpha compositing of [overlay] onto [base]; [base] is always treated as opaque. */
    private fun alphaOver(overlay: Int, base: Int): Int {
        val overlayAlpha = ((overlay ushr 24) and 0xFF) / 255.0
        if (overlayAlpha <= 0.0) return base

        val baseR = (base ushr 16) and 0xFF
        val baseG = (base ushr 8) and 0xFF
        val baseB = base and 0xFF
        val overlayR = (overlay ushr 16) and 0xFF
        val overlayG = (overlay ushr 8) and 0xFF
        val overlayB = overlay and 0xFF

        val r = (overlayR * overlayAlpha + baseR * (1 - overlayAlpha)).toInt()
        val g = (overlayG * overlayAlpha + baseG * (1 - overlayAlpha)).toInt()
        val b = (overlayB * overlayAlpha + baseB * (1 - overlayAlpha)).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun wrapTexel(value: Double, size: Int): Int = value.mod(size.toDouble()).toInt().coerceIn(0, size - 1)
}
