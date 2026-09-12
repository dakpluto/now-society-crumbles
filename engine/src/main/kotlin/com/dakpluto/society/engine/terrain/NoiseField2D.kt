package com.dakpluto.society.engine.terrain

import kotlin.math.floor
import kotlin.random.Random

/**
 * A deterministic 2D Perlin noise field, seeded independently of [SimRandom] -
 * it's sampled at arbitrary (x, y) coordinates rather than drawn from
 * sequentially, so it doesn't fit SimRandom's roll-by-roll API. The seed
 * itself is expected to come from a SimRandom draw upstream (see
 * TerrainGenerator), keeping the run's overall determinism chain intact.
 *
 * [frequency] controls feature size: lower values produce larger, smoother
 * contiguous regions (a sprawling mountain range); higher values produce
 * tighter, noisier variation.
 */
class NoiseField2D(seed: Long, private val frequency: Double = 0.08) {
    private val permutation: IntArray = run {
        val table = IntArray(256) { it }
        val random = Random(seed)
        for (i in 255 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = table[i]
            table[i] = table[j]
            table[j] = tmp
        }
        IntArray(512) { table[it and 255] }
    }

    /**
     * Raw [perlin] output does reach the full [-1, 1] range, but nowhere near
     * uniformly - like any Perlin noise, its values cluster heavily around
     * 0 (a bell curve, not a flat distribution). A naive linear `(raw+1)/2`
     * normalization preserves that clustering, which silently breaks
     * [TerrainField]'s "uniformly spaced" placeholder blend: elements near
     * the middle of a category's declaration order end up dramatically more
     * common than elements at the ends (e.g. Sand and Peat/Bog in Ground
     * composition would barely ever appear, even though their blend range
     * is nominally the same width as every other element's).
     *
     * Fixed via a rank/percentile transform (histogram equalization):
     * densely sample several periods of the raw noise once per instance
     * (seed-deterministic, independent of [frequency] and of whatever map
     * size ends up sampling this field) and sort them, then [sample] maps
     * any raw value to its percentile within that reference distribution
     * instead of linearly rescaling it. That flattens the *output*
     * distribution to genuinely uniform on [0, 1] regardless of the raw
     * noise's real shape, so every element in a category ends up with
     * roughly equal odds of appearing, matching what "uniformly spaced"
     * was always meant to guarantee.
     */
    private val calibrationSamples: DoubleArray = run {
        val periods = 8
        val steps = periods * 32
        val domain = periods.toDouble() / frequency
        val step = domain / steps

        val samples = DoubleArray(steps * steps)
        for (ix in 0 until steps) {
            for (iy in 0 until steps) {
                samples[ix * steps + iy] = perlin(ix * step * frequency, iy * step * frequency)
            }
        }
        samples.sortedArray()
    }

    /** Noise value at (x, y), normalized to [0.0, 1.0] and uniformly distributed there. */
    fun sample(x: Double, y: Double): Double {
        val raw = perlin(x * frequency, y * frequency)
        val insertionPoint = calibrationSamples.binarySearch(raw)
        val rank = if (insertionPoint >= 0) insertionPoint else -(insertionPoint + 1)
        return (rank.toDouble() / (calibrationSamples.size - 1)).coerceIn(0.0, 1.0)
    }

    private fun perlin(x: Double, y: Double): Double {
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255
        val xf = x - floor(x)
        val yf = y - floor(y)
        val u = fade(xf)
        val v = fade(yf)

        val aa = permutation[permutation[xi] + yi]
        val ab = permutation[permutation[xi] + yi + 1]
        val ba = permutation[permutation[xi + 1] + yi]
        val bb = permutation[permutation[xi + 1] + yi + 1]

        val x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u)
        val x2 = lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u)
        return lerp(x1, x2, v)
    }

    private fun fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

    private fun lerp(a: Double, b: Double, t: Double): Double = a + t * (b - a)

    private fun grad(hash: Int, x: Double, y: Double): Double = when (hash and 3) {
        0 -> x + y
        1 -> -x + y
        2 -> x - y
        else -> -x - y
    }
}
