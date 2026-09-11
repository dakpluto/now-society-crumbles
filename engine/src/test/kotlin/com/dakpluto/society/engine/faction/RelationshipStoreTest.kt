package com.dakpluto.society.engine.faction

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RelationshipStoreTest : StringSpec({
    val factionA = FactionId("A")
    val factionB = FactionId("B")

    "an unset relationship defaults to NEUTRAL" {
        val store = RelationshipStore()

        store.relationshipOf(factionA, factionB) shouldBe FactionRelationship.NEUTRAL
    }

    "set then get round-trips" {
        val store = RelationshipStore()
        val relationship = FactionRelationship(trust = 80.0, trade = 20.0, militaryTension = 60.0)

        store.setRelationship(factionA, factionB, relationship)

        store.relationshipOf(factionA, factionB) shouldBe relationship
    }

    "relationships are asymmetric - setting one direction leaves the reverse untouched" {
        val store = RelationshipStore()
        val relationship = FactionRelationship(trust = 80.0, trade = 20.0, militaryTension = 60.0)

        store.setRelationship(factionA, factionB, relationship)

        store.relationshipOf(factionB, factionA) shouldBe FactionRelationship.NEUTRAL
    }

    "a faction has no relationship toward itself" {
        val store = RelationshipStore()

        shouldThrow<IllegalArgumentException> { store.relationshipOf(factionA, factionA) }
        shouldThrow<IllegalArgumentException> { store.setRelationship(factionA, factionA, FactionRelationship.NEUTRAL) }
    }
})
