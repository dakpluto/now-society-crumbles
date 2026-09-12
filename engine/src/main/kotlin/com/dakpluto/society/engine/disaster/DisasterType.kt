package com.dakpluto.society.engine.disaster

/**
 * The natural disaster roster (human-caused events use separate formulas -
 * see CONCEPT.md -> "Human-caused events"). See CONCEPT.md -> "Disasters &
 * Events" -> "Natural disasters" -> "Disaster roster", tagged against
 * [SpreadPattern] per "Spread pattern taxonomy".
 *
 * Epidemic is one roster entry with three variants sharing the same
 * mechanical scaffolding but hitting different stats (crop -> farming
 * yield, livestock -> food supply/economy, human -> population/labor
 * directly) - kept as three separate entries here since spread pattern and
 * everything else this enum carries applies identically to each.
 *
 * Two gaps in CONCEPT.md itself, not decided here: [SANDSTORM] isn't tagged
 * against any pattern anywhere in "Spread pattern taxonomy" (roster
 * inclusion only), so it gets an empty [spreadPatterns] rather than a
 * guessed classification. [TSUNAMI] is only partially classified: the
 * taxonomy's "Composites" paragraph names Radial falloff as one component
 * (borrowed from the triggering Earthquake/Meteor's origin), but its other
 * component - "coastal-adjacency propagation" - isn't one of the six named
 * patterns, so only the Radial falloff half is captured here. Worth a
 * follow-up pass on CONCEPT.md for both.
 */
enum class DisasterType(val spreadPatterns: Set<SpreadPattern>) {
    DROUGHT(setOf(SpreadPattern.BROAD_AREA)),
    SANDSTORM(emptySet()),
    HURRICANE_CYCLONE(setOf(SpreadPattern.DIRECTIONAL_TRACK)),
    FLOOD(setOf(SpreadPattern.GRADIENT_FLOW)),
    EARTHQUAKE(setOf(SpreadPattern.RADIAL_FALLOFF)),
    WILDFIRE(setOf(SpreadPattern.DIRECTIONAL_TRACK)),
    BLIZZARD(setOf(SpreadPattern.BROAD_AREA)),
    TORNADO(setOf(SpreadPattern.DIRECTIONAL_TRACK)),
    VOLCANIC_ERUPTION(setOf(SpreadPattern.GRADIENT_FLOW, SpreadPattern.DIRECTIONAL_TRACK)),
    LANDSLIDE(setOf(SpreadPattern.GRADIENT_FLOW)),
    TSUNAMI(setOf(SpreadPattern.RADIAL_FALLOFF)),
    AVALANCHE(setOf(SpreadPattern.GRADIENT_FLOW)),
    SINKHOLE(setOf(SpreadPattern.POINT_LOCALIZED)),
    EXTREME_HEAT(setOf(SpreadPattern.BROAD_AREA)),
    METEOR(setOf(SpreadPattern.RADIAL_FALLOFF)),
    EPIDEMIC_CROP(setOf(SpreadPattern.CONTACT_CONNECTIVITY_DRIVEN)),
    EPIDEMIC_LIVESTOCK(setOf(SpreadPattern.CONTACT_CONNECTIVITY_DRIVEN)),
    EPIDEMIC_HUMAN(setOf(SpreadPattern.CONTACT_CONNECTIVITY_DRIVEN)),
    ;

    companion object {
        /** Types tagged against more than one pattern simultaneously - see "Composites" in CONCEPT.md. */
        val COMPOSITE_TYPES: Set<DisasterType> by lazy { entries.filter { it.spreadPatterns.size > 1 }.toSet() }

        /** Types CONCEPT.md's spread-pattern taxonomy doesn't classify at all - see the class-level doc gap note. */
        val UNCLASSIFIED_TYPES: Set<DisasterType> by lazy { entries.filter { it.spreadPatterns.isEmpty() }.toSet() }
    }
}
