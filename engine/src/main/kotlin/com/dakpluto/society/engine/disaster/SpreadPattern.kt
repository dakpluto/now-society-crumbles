package com.dakpluto.society.engine.disaster

/**
 * Reusable disaster spread-pattern shapes, generalized from Avalanche's
 * origin-vs-spread structure rather than a bespoke rule per disaster type.
 * See CONCEPT.md -> "Spread pattern taxonomy". Exact per-pattern math (decay
 * rate, track width, etc.) is still TBD - this only fixes which pattern
 * shape applies to which [DisasterType].
 */
enum class SpreadPattern {
    /** Origin point; impact grows moving downhill along the local elevation gradient, away from the origin. */
    GRADIENT_FLOW,

    /** Origin point; intensity decreases with radial distance in all directions. */
    RADIAL_FALLOFF,

    /** Origin point; moves along a path in a dominant direction (wind, storm movement), weakening over distance/time. */
    DIRECTIONAL_TRACK,

    /** No single origin point; affects a contiguous region weighted by terrain suitability, no directional growth. */
    BROAD_AREA,

    /** Minimal-to-no spread beyond the origin cell or a tight cluster. */
    POINT_LOCALIZED,

    /** Spreads along occupancy/trade proximity between cells and factions, not a physical terrain gradient. */
    CONTACT_CONNECTIVITY_DRIVEN,
}
