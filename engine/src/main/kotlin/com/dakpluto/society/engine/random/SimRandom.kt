package com.dakpluto.society.engine.random

import kotlin.random.Random

/**
 * The sim's single source of randomness. Every random roll - variance noise,
 * disaster generation, regression checks - goes through a SimRandom seeded at
 * simulation setup, so a run is fully reproducible from its seed alone. See
 * CONCEPT.md -> Technical Approach -> "All randomness routed through a seeded
 * PRNG".
 *
 * Deliberately minimal for now: the universal chaos-constant probability
 * floor (CONCEPT.md -> Disasters & Events -> "Universal chaos constant") is
 * per-run rolled state that belongs to whatever holds simulation state once
 * that's built, not to this stateless wrapper - callers apply it themselves
 * until then.
 */
class SimRandom(val seed: Long) {
    private val random = Random(seed)

    /** Uniform double in [0.0, 1.0). */
    fun nextDouble(): Double = random.nextDouble()

    /** Uniform double in [from, until). */
    fun nextDouble(from: Double, until: Double): Double = random.nextDouble(from, until)

    /** Uniform int in [0, until). */
    fun nextInt(until: Int): Int = random.nextInt(until)

    /** Uniform int in [from, until). */
    fun nextInt(from: Int, until: Int): Int = random.nextInt(from, until)

    /** Uniform long across the full Long range - used to derive sub-seeds for other seeded generators (e.g. per-category noise fields). */
    fun nextLong(): Long = random.nextLong()

    /**
     * True with the given [probability] (0.0-1.0) - the common primitive
     * behind every occurrence/regression/event roll described in CONCEPT.md
     * (e.g. "if the computed risk is 5%, the stat regresses if a roll lands
     * in the top 5% of the range").
     */
    fun chance(probability: Double): Boolean = nextDouble() < probability
}
