package com.dakpluto.society.ui.map

import com.dakpluto.society.engine.terrain.TerrainCategory
import com.dakpluto.society.engine.terrain.TerrainElement
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun alphaOf(argb: Int): Int = (argb ushr 24) and 0xFF

class TerrainTexturesTest : StringSpec({
    val renderableElements = TerrainElement.entries.filterNot { it.category == TerrainCategory.TOPOGRAPHY }

    "every ground, vegetation, and water element resolves to a full-size tile" {
        for (element in renderableElements) {
            TerrainTextures.tileFor(element).size shouldBe TerrainTextures.TILE_SIZE * TerrainTextures.TILE_SIZE
        }
    }

    "Barren and Landlocked - the 'no overlay' elements - are fully transparent" {
        TerrainTextures.tileFor(TerrainElement.BARREN).all { alphaOf(it) == 0 } shouldBe true
        TerrainTextures.tileFor(TerrainElement.LANDLOCKED).all { alphaOf(it) == 0 } shouldBe true
    }

    "Ground composition elements are always fully opaque, real art or fallback" {
        for (element in TerrainElement.of(TerrainCategory.GROUND_COMPOSITION)) {
            TerrainTextures.tileFor(element).all { alphaOf(it) == 255 } shouldBe true
        }
    }

    "tileFor() is deterministic" {
        for (element in renderableElements) {
            TerrainTextures.tileFor(element) shouldBe TerrainTextures.tileFor(element)
        }
    }
})
