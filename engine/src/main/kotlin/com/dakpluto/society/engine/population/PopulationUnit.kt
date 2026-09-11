package com.dakpluto.society.engine.population

import com.dakpluto.society.engine.random.SimRandom

/**
 * One simulated population unit: [tier] determines how much a stat access
 * gets randomized around its [baseStats] value. See CONCEPT.md ->
 * "Population Model: Unique Units". Stats are flat, uniformly-typed values
 * keyed by name (personality traits, knowledge, current-condition readouts
 * alike) per the Stat System's "flat and uniform under the hood" principle -
 * TODO: key by a finalized Stat enum once the master stat list settles (see
 * CONCEPT.md -> Stat System -> "Master stat list (draft)").
 */
data class PopulationUnit(val tier: UnitTier, val baseStats: Map<String, Double>) {
    /**
     * The value of [stat] for this use, with tier-appropriate variance
     * freshly rolled. Per CONCEPT.md's "Implementation notes", this is
     * recalculated every call - never cached, and never feeds back into
     * [baseStats]. The base value only ever changes through persistent
     * causes applied elsewhere (education, disasters, etc.), not through
     * this per-calculation noise.
     */
    fun effectiveStat(stat: String, random: SimRandom): Double {
        val base = baseStats.getValue(stat)
        val range = tier.variancePercentRange
        if (range.start == 0.0 && range.endInclusive == 0.0) return base

        val magnitude = random.nextDouble(range.start, range.endInclusive) * base
        val sign = if (random.chance(0.5)) 1.0 else -1.0
        return base + sign * magnitude
    }
}
