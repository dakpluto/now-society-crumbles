package com.dakpluto.society.engine.terrain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

class TerrainFieldTest : StringSpec({
    "the same seed produces identical weights at the same point" {
        val a = TerrainField(seed = 42L, frequency = 0.08)
        val b = TerrainField(seed = 42L, frequency = 0.08)

        a.elementWeightsAt(3.5, 7.25) shouldBe b.elementWeightsAt(3.5, 7.25)
    }

    "category weights sum to 1.0 at an arbitrary non-integer point" {
        val field = TerrainField(seed = 5L, frequency = 0.08)

        for (category in TerrainCategory.entries) {
            val total = field.categoryWeightsAt(category, 12.375, 4.625).values.sum()
            total shouldBe (1.0 plusOrMinus 1e-9)
        }
    }

    "sampling is continuous - nearby sub-cell points are close, not snapped to a grid" {
        val field = TerrainField(seed = 9L, frequency = 0.08)

        val here = field.categoryWeightsAt(TerrainCategory.VEGETATION_COVER, 10.0, 10.0)
        val near = field.categoryWeightsAt(TerrainCategory.VEGETATION_COVER, 10.05, 10.0)

        for (element in TerrainElement.of(TerrainCategory.VEGETATION_COVER)) {
            val delta = kotlin.math.abs((here[element] ?: 0.0) - (near[element] ?: 0.0))
            delta shouldBeLessThan 0.1
        }
    }
})
