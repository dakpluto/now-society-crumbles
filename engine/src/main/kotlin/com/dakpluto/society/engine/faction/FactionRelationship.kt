package com.dakpluto.society.engine.faction

/**
 * One faction's standing toward another, tracked as three independent
 * dimensions on the sim's default 1-100 scale. See CONCEPT.md ->
 * "Relationship structure (faction-to-faction)":
 * - [trust]: general goodwill/perceived reliability - gates aid-rendering
 *   and alliance willingness.
 * - [trade]: economic relationship strength/volume.
 * - [militaryTension]: hostility/perceived threat.
 *
 * Diplomatic Influence and ideological affinity are explicitly *not* stored
 * here - CONCEPT.md resolves them as derived values (computed from this
 * structure, and from each faction's own Economic/Social Ideology stats,
 * respectively) rather than a fourth tracked dimension. Their exact
 * derivation formulas are still open, so there's nothing to implement yet.
 */
data class FactionRelationship(
    val trust: Double,
    val trade: Double,
    val militaryTension: Double,
) {
    init {
        require(trust in SCALE) { "trust must be in $SCALE, was $trust" }
        require(trade in SCALE) { "trade must be in $SCALE, was $trade" }
        require(militaryTension in SCALE) { "militaryTension must be in $SCALE, was $militaryTension" }
    }

    companion object {
        private val SCALE = 1.0..100.0

        /**
         * Midpoint default for a faction pair with no relationship history
         * yet. CONCEPT.md doesn't specify initial-state numbers for a fresh
         * simulation's starting factions, so this is a neutral placeholder.
         */
        val NEUTRAL = FactionRelationship(trust = 50.0, trade = 50.0, militaryTension = 50.0)
    }
}
