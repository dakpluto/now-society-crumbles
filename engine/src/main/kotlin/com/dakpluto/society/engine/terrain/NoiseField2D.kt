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

    /** Noise value at (x, y), normalized to [0.0, 1.0]. */
    fun sample(x: Double, y: Double): Double {
        val raw = perlin(x * frequency, y * frequency)
        return ((raw + 1.0) / 2.0).coerceIn(0.0, 1.0)
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
