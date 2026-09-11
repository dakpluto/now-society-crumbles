package com.dakpluto.society.engine.faction

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FactionRelationshipTest : StringSpec({
    "constructs with valid values on the 1-100 scale" {
        val relationship = FactionRelationship(trust = 1.0, trade = 100.0, militaryTension = 50.0)

        relationship.trust shouldBe 1.0
        relationship.trade shouldBe 100.0
        relationship.militaryTension shouldBe 50.0
    }

    "rejects trust outside the 1-100 scale" {
        shouldThrow<IllegalArgumentException> { FactionRelationship(trust = 0.0, trade = 50.0, militaryTension = 50.0) }
        shouldThrow<IllegalArgumentException> { FactionRelationship(trust = 100.1, trade = 50.0, militaryTension = 50.0) }
    }

    "rejects trade outside the 1-100 scale" {
        shouldThrow<IllegalArgumentException> { FactionRelationship(trust = 50.0, trade = 0.0, militaryTension = 50.0) }
        shouldThrow<IllegalArgumentException> { FactionRelationship(trust = 50.0, trade = 100.1, militaryTension = 50.0) }
    }

    "rejects militaryTension outside the 1-100 scale" {
        shouldThrow<IllegalArgumentException> { FactionRelationship(trust = 50.0, trade = 50.0, militaryTension = 0.0) }
        shouldThrow<IllegalArgumentException> { FactionRelationship(trust = 50.0, trade = 50.0, militaryTension = 100.1) }
    }

    "NEUTRAL is a valid midpoint relationship" {
        FactionRelationship.NEUTRAL shouldBe FactionRelationship(trust = 50.0, trade = 50.0, militaryTension = 50.0)
    }
})
