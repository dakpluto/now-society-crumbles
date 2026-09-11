package com.dakpluto.society.engine.population

import com.dakpluto.society.engine.random.SimRandom
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

class PopulationUnitTest : StringSpec({
    "a Person unit's effective stat is always exactly the base value" {
        val unit = PopulationUnit(UnitTier.PERSON, mapOf("FarmingKnowledge" to 52.0))
        val random = SimRandom(1L)

        repeat(1000) {
            unit.effectiveStat("FarmingKnowledge", random) shouldBe 52.0
        }
    }

    "a Family unit's effective stat stays within its variance band and actually varies" {
        val base = 52.0
        val unit = PopulationUnit(UnitTier.FAMILY, mapOf("FarmingKnowledge" to base))
        val random = SimRandom(2L)
        val maxSwing = base * UnitTier.FAMILY.variancePercentRange.endInclusive

        val values = (1..1000).map { unit.effectiveStat("FarmingKnowledge", random) }

        for (value in values) {
            value shouldBeGreaterThanOrEqual (base - maxSwing)
            value shouldBeLessThanOrEqual (base + maxSwing)
        }
        values.toSet() shouldHaveAtLeastSize 2
    }

    "a Community unit's effective stat stays within its (wider) variance band" {
        val base = 52.0
        val unit = PopulationUnit(UnitTier.COMMUNITY, mapOf("FarmingKnowledge" to base))
        val random = SimRandom(3L)
        val maxSwing = base * UnitTier.COMMUNITY.variancePercentRange.endInclusive

        repeat(1000) {
            val value = unit.effectiveStat("FarmingKnowledge", random)
            value shouldBeGreaterThanOrEqual (base - maxSwing)
            value shouldBeLessThanOrEqual (base + maxSwing)
        }
    }

    "the base value is never mutated by reading effectiveStat" {
        val unit = PopulationUnit(UnitTier.COMMUNITY, mapOf("FarmingKnowledge" to 52.0))
        val random = SimRandom(4L)

        repeat(100) { unit.effectiveStat("FarmingKnowledge", random) }

        unit.baseStats["FarmingKnowledge"] shouldBe 52.0
    }
})
