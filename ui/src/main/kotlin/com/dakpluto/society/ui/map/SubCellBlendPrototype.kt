package com.dakpluto.society.ui.map

import com.dakpluto.society.engine.terrain.TerrainCategory
import com.dakpluto.society.engine.terrain.TerrainElement
import com.dakpluto.society.engine.terrain.TerrainField
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import kotlin.math.floor

/**
 * Prototype for smooth, continuous terrain-texture blending - CONCEPT.md's
 * "Cell rendering derives from terrain element composition", extended past
 * the flat-color `TerrainColors` placeholder toward real photo textures.
 *
 * The problem this validates: once a cell renders as a real texture instead
 * of a flat color, sampling the terrain field once per cell (as
 * `TerrainGenerator`/`MapView` do today) produces a hard, unrealistic edge
 * at every cell boundary where the blend ratio jumps to the next cell's
 * value. `TerrainField` is continuous, though - it varies smoothly at any
 * (x, y), not just integer cell coordinates - so sampling it at render
 * resolution (per pixel here) instead of once per cell removes that edge
 * entirely: the blend ratio itself changes gradually across the whole map.
 *
 * [render]'s `sampleGranularityPx` isolates that one variable for
 * comparison: a large value (e.g. one cell's width) snaps the blend ratio
 * to one sample per cell - reproducing today's hard-edge behavior for
 * reference - while a small value (the default, 1.0) samples continuously.
 * Texture detail itself is always drawn at full per-pixel resolution
 * either way; only the *blend ratio*'s sampling rate changes.
 *
 * Deliberately narrow scope: only Vegetation cover's Forest/Trees weight is
 * textured (blended against a Sand base), since those are the only two real
 * textures on hand so far - this validates the technique, not the full
 * multi-category rendering pipeline (`TerrainColors` covers that today).
 *
 * Works on raw ARGB int arrays throughout (bulk-read the source textures
 * once, blend into a plain IntArray, bulk-write it to the output image
 * once) rather than per-pixel `Color` objects - `PixelReader.getColor`/
 * `PixelWriter.setColor` allocate a `Color` per call and are far too slow
 * for a few hundred thousand pixels.
 */
object SubCellBlendPrototype {
    private val sandImage = Image(javaClass.getResourceAsStream("/textures/sand.png"))
    private val forestImage = Image(javaClass.getResourceAsStream("/textures/forest.png"))
    private val sandArgb = readArgb(sandImage)
    private val forestArgb = readArgb(forestImage)

    fun render(
        field: TerrainField,
        widthCells: Int,
        heightCells: Int,
        pixelsPerCell: Double,
        cellsPerTextureTile: Double,
        sampleGranularityPx: Double = 1.0,
    ): WritableImage {
        val widthPx = (widthCells * pixelsPerCell).toInt()
        val heightPx = (heightCells * pixelsPerCell).toInt()
        val texelsPerWorldUnit = sandImage.width / cellsPerTextureTile
        val textureSize = sandImage.width.toInt()

        val outputArgb = IntArray(widthPx * heightPx)
        for (py in 0 until heightPx) {
            val sampleWorldY = snap(py, sampleGranularityPx) / pixelsPerCell
            val texelY = wrapTexel(py / pixelsPerCell * texelsPerWorldUnit, textureSize)
            val rowOffset = py * widthPx

            for (px in 0 until widthPx) {
                val sampleWorldX = snap(px, sampleGranularityPx) / pixelsPerCell
                val forestWeight = field
                    .categoryWeightsAt(TerrainCategory.VEGETATION_COVER, sampleWorldX, sampleWorldY)[TerrainElement.FOREST_TREES]
                    ?: 0.0

                val texelX = wrapTexel(px / pixelsPerCell * texelsPerWorldUnit, textureSize)
                val texelIndex = texelY * textureSize + texelX

                outputArgb[rowOffset + px] = blendArgb(sandArgb[texelIndex], forestArgb[texelIndex], forestWeight)
            }
        }

        val output = WritableImage(widthPx, heightPx)
        output.pixelWriter.setPixels(0, 0, widthPx, heightPx, PixelFormat.getIntArgbInstance(), outputArgb, 0, widthPx)
        return output
    }

    private fun readArgb(image: Image): IntArray {
        val width = image.width.toInt()
        val height = image.height.toInt()
        val buffer = IntArray(width * height)
        image.pixelReader.getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), buffer, 0, width)
        return buffer
    }

    private fun blendArgb(from: Int, to: Int, t: Double): Int {
        val fromA = (from ushr 24) and 0xFF
        val fromR = (from ushr 16) and 0xFF
        val fromG = (from ushr 8) and 0xFF
        val fromB = from and 0xFF
        val toA = (to ushr 24) and 0xFF
        val toR = (to ushr 16) and 0xFF
        val toG = (to ushr 8) and 0xFF
        val toB = to and 0xFF

        val a = (fromA + (toA - fromA) * t).toInt()
        val r = (fromR + (toR - fromR) * t).toInt()
        val g = (fromG + (toG - fromG) * t).toInt()
        val b = (fromB + (toB - fromB) * t).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun snap(pixel: Int, granularityPx: Double): Double = floor(pixel / granularityPx) * granularityPx

    private fun wrapTexel(value: Double, size: Int): Int = value.mod(size.toDouble()).toInt().coerceIn(0, size - 1)
}
