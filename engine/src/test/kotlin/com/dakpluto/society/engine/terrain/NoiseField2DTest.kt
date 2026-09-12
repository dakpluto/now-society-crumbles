package com.dakpluto.society.engine.terrain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual as intShouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual as intShouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class NoiseField2DTest : StringSpec({
    "the same seed produces the identical field" {
        val a = NoiseField2D(seed = 42L)
        val b = NoiseField2D(seed = 42L)

        val samplesA = (0 until 20).map { x -> (0 until 20).map { y -> a.sample(x.toDouble(), y.toDouble()) } }
        val samplesB = (0 until 20).map { x -> (0 until 20).map { y -> b.sample(x.toDouble(), y.toDouble()) } }

        samplesA shouldBe samplesB
    }

    "different seeds produce different fields" {
        val a = NoiseField2D(seed = 1L)
        val b = NoiseField2D(seed = 2L)

        val samplesA = (0 until 20).map { x -> (0 until 20).map { y -> a.sample(x.toDouble(), y.toDouble()) } }
        val samplesB = (0 until 20).map { x -> (0 until 20).map { y -> b.sample(x.toDouble(), y.toDouble()) } }

        samplesA shouldNotBe samplesB
    }

    "sample() always falls in [0.0, 1.0]" {
        val field = NoiseField2D(seed = 7L)
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                val value = field.sample(x.toDouble(), y.toDouble())
                value shouldBeGreaterThanOrEqual 0.0
                value shouldBeLessThanOrEqual 1.0
            }
        }
    }

    "adjacent samples are close together (smooth, not salt-and-pepper noise)" {
        val field = NoiseField2D(seed = 9L, frequency = 0.08)
        for (x in 0 until 50) {
            val here = field.sample(x.toDouble(), 0.0)
            val next = field.sample((x + 1).toDouble(), 0.0)
            kotlin.math.abs(next - here) shouldBeLessThanOrEqual 0.25
        }
    }

    "sample() output is roughly uniform across [0.0, 1.0], not clustered around 0.5" {
        // Regression test for a real bug: a naive linear (raw+1)/2 normalization
        // preserves raw Perlin's bell-curve clustering around the middle, which
        // starves whichever elements sit at the ends of a TerrainField category's
        // blend order (e.g. Sand and Peat/Bog in Ground composition) - see
        // NoiseField2D's own doc comment on `calibrationSamples`.
        val field = NoiseField2D(seed = 13L, frequency = 0.1)
        val bins = IntArray(10)
        val samplesPerAxis = 200
        for (x in 0 until samplesPerAxis) {
            for (y in 0 until samplesPerAxis) {
                val value = field.sample(x.toDouble(), y.toDouble())
                val bin = (value * bins.size).toInt().coerceIn(0, bins.size - 1)
                bins[bin]++
            }
        }

        val total = samplesPerAxis * samplesPerAxis
        val expectedPerBin = total / bins.size
        for (count in bins) {
            count.intShouldBeGreaterThanOrEqual((expectedPerBin * 0.6).toInt())
            count.intShouldBeLessThanOrEqual((expectedPerBin * 1.4).toInt())
        }
    }
})
