package com.dakpluto.society.engine.faction

/**
 * Per-ordered-pair faction relationships. Asymmetric by design - a faction's
 * relationship toward another is tracked and read independently of the
 * reverse direction. See CONCEPT.md -> "Relationship structure
 * (faction-to-faction)".
 *
 * Updates (trade, aid, conflict, etc. adjusting these dimensions) are a
 * future concern for whatever runs the Need -> Strategy -> Resolution loop;
 * this is just the storage.
 */
class RelationshipStore {
    private val relationships = mutableMapOf<Pair<FactionId, FactionId>, FactionRelationship>()

    /** [subject]'s relationship toward [target] - [FactionRelationship.NEUTRAL] if never set. */
    fun relationshipOf(subject: FactionId, target: FactionId): FactionRelationship {
        require(subject != target) { "a faction has no relationship toward itself" }
        return relationships[subject to target] ?: FactionRelationship.NEUTRAL
    }

    /** Sets [subject]'s relationship toward [target]. Does not affect the reverse direction. */
    fun setRelationship(subject: FactionId, target: FactionId, relationship: FactionRelationship) {
        require(subject != target) { "a faction has no relationship toward itself" }
        relationships[subject to target] = relationship
    }
}
