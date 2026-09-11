package com.dakpluto.society.engine.population

/**
 * How much internal diversity a population unit's stats have, per CONCEPT.md
 * -> "Population Model: Unique Units" -> "Variance bands". A Person unit
 * represents exactly one individual (no internal diversity, 0% variance); a
 * Family or Community unit represents an aggregate, so a stat access
 * randomizes within a tier-specific swing around the base value instead of
 * using it exactly.
 *
 * [variancePercentRange] is the range a single stat access's swing magnitude
 * is drawn from (e.g. Family: somewhere between 1% and 5% of the base value,
 * in a random direction) - discrete v1 bands per CONCEPT.md's implementation
 * notes, not a continuous formula.
 */
enum class UnitTier(val variancePercentRange: ClosedFloatingPointRange<Double>) {
    PERSON(0.0..0.0),
    FAMILY(0.01..0.05),
    COMMUNITY(0.05..0.20),
}
