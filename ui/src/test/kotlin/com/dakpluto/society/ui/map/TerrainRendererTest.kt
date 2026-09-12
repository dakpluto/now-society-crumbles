package com.dakpluto.society.ui.map

import com.dakpluto.society.engine.terrain.TerrainField
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage

private fun pixelsOf(image: WritableImage): IntArray {
    val width = image.width.toInt()
    val height = image.height.toInt()
    val buffer = IntArray(width * height)
    image.pixelReader.getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), buffer, 0, width)
    return buffer
}

class TerrainRendererTest : StringSpec({
    "render() is deterministic for the same field" {
        val field = TerrainField(seed = 7L, frequency = 0.1)

        val a = TerrainRenderer.render(field, widthCells = 8, heightCells = 6, pixelsPerCell = 4.0, cellsPerTextureTile = 2.0)
        val b = TerrainRenderer.render(field, widthCells = 8, heightCells = 6, pixelsPerCell = 4.0, cellsPerTextureTile = 2.0)

        pixelsOf(a) shouldBe pixelsOf(b)
    }

    "render() produces an opaque image sized to widthCells/heightCells x pixelsPerCell" {
        val field = TerrainField(seed = 3L, frequency = 0.1)

        val image = TerrainRenderer.render(field, widthCells = 5, heightCells = 4, pixelsPerCell = 10.0, cellsPerTextureTile = 2.0)

        image.width shouldBe 50.0
        image.height shouldBe 40.0
        pixelsOf(image).all { (it ushr 24) and 0xFF == 255 } shouldBe true
    }
})
