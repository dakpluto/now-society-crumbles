package com.dakpluto.society.ui.map

import com.dakpluto.society.engine.terrain.TerrainCategory
import com.dakpluto.society.engine.terrain.TerrainField
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Dev-only tool: renders a single [TerrainCategory]'s continuous blend at
 * whatever scale is asked for, in isolation from every other category (no
 * vegetation/water overlay, no topography brightness) - the point is to see
 * one category's own art/zones clearly at scale while adding new textures
 * (see ASSETS.md), not a realistic composite. For that, run the real app.
 *
 * Not part of the shipped app - run via `./gradlew :ui:renderPreview`
 * (see ui/build.gradle.kts), which stays separate from the `application`/
 * `run` task so this never displaces [com.dakpluto.society.ui.App] as the
 * project's actual entry point.
 *
 * Vegetation cover and Water features are transparent overlays (Barren and
 * Landlocked render fully transparent - see [TerrainTextures]), so a
 * category rendered alone would otherwise come out as raw alpha, which
 * different image viewers flatten against different backgrounds (some
 * black, some white) - not what the art actually looks like once composited.
 * This flattens every output against a fixed neutral grey backdrop so it
 * reads consistently everywhere.
 *
 * All parameters are optional `--name=value` args (e.g. `--category=water_features
 * --seed=7 --width=200`); see [PreviewOptions] for the full list and defaults.
 */
private const val BACKDROP_ARGB = 0xFFAAAAAA.toInt()

fun main(args: Array<String>) {
    val options = PreviewOptions.parse(args)
    val field = TerrainField(seed = options.seed, frequency = options.frequency)
    val tileSize = TerrainTextures.TILE_SIZE
    val texelsPerWorldUnit = tileSize / options.cellsPerTextureTile
    val widthPx = (options.widthCells * options.pixelsPerCell).toInt()
    val heightPx = (options.heightCells * options.pixelsPerCell).toInt()

    val image = BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_ARGB)
    for (py in 0 until heightPx) {
        val worldY = py / options.pixelsPerCell
        val texelY = wrapTexel(worldY * texelsPerWorldUnit, tileSize)

        for (px in 0 until widthPx) {
            val worldX = px / options.pixelsPerCell
            val texelX = wrapTexel(worldX * texelsPerWorldUnit, tileSize)
            val texelIndex = texelY * tileSize + texelX

            val overlay = blendCategoryArgb(field, options.category, worldX, worldY, texelIndex)
            image.setRGB(px, py, alphaOverBackdrop(overlay))
        }
    }

    options.outputFile.parentFile?.mkdirs()
    ImageIO.write(image, "png", options.outputFile)
    println("Wrote ${options.outputFile.absolutePath}")
}

private fun alphaOverBackdrop(overlay: Int): Int {
    val overlayAlpha = ((overlay ushr 24) and 0xFF) / 255.0
    if (overlayAlpha >= 1.0) return overlay

    val backdropR = (BACKDROP_ARGB ushr 16) and 0xFF
    val backdropG = (BACKDROP_ARGB ushr 8) and 0xFF
    val backdropB = BACKDROP_ARGB and 0xFF
    val overlayR = (overlay ushr 16) and 0xFF
    val overlayG = (overlay ushr 8) and 0xFF
    val overlayB = overlay and 0xFF

    val r = (overlayR * overlayAlpha + backdropR * (1 - overlayAlpha)).toInt()
    val g = (overlayG * overlayAlpha + backdropG * (1 - overlayAlpha)).toInt()
    val b = (overlayB * overlayAlpha + backdropB * (1 - overlayAlpha)).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

/** Weighted sum of every nonzero-weight element's texel in [category] - at most two, per [TerrainField]. */
private fun blendCategoryArgb(
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

private fun wrapTexel(value: Double, size: Int): Int = value.mod(size.toDouble()).toInt().coerceIn(0, size - 1)

private class PreviewOptions(
    val category: TerrainCategory,
    val widthCells: Int,
    val heightCells: Int,
    val seed: Long,
    val frequency: Double,
    val pixelsPerCell: Double,
    val cellsPerTextureTile: Double,
    val outputFile: File,
) {
    companion object {
        fun parse(args: Array<String>): PreviewOptions {
            val named = args.mapNotNull { arg ->
                val eq = arg.indexOf('=')
                if (!arg.startsWith("--") || eq < 0) null else arg.substring(2, eq) to arg.substring(eq + 1)
            }.toMap()

            val category = named["category"]?.let { TerrainCategory.valueOf(it.uppercase()) }
                ?: TerrainCategory.GROUND_COMPOSITION

            return PreviewOptions(
                category = category,
                widthCells = named["width"]?.toInt() ?: 500,
                heightCells = named["height"]?.toInt() ?: 500,
                seed = named["seed"]?.toLong() ?: 42L,
                frequency = named["frequency"]?.toDouble() ?: 0.008,
                pixelsPerCell = named["pixelsPerCell"]?.toDouble() ?: 2.0,
                cellsPerTextureTile = named["cellsPerTextureTile"]?.toDouble() ?: 2.0,
                outputFile = File(named["out"] ?: "build/preview/${category.name.lowercase()}.png"),
            )
        }
    }
}
