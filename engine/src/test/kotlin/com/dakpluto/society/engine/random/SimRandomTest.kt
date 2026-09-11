package com.dakpluto.society.engine.random

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

class SimRandomTest : StringSpec({
    "the same seed produces the identical sequence of rolls" {
        checkAll(Arb.long()) { seed ->
            val a = SimRandom(seed)
            val b = SimRandom(seed)

            List(50) { a.nextDouble() } shouldBe List(50) { b.nextDouble() }
        }
    }

    "nextDouble() always falls in [0.0, 1.0)" {
        val random = SimRandom(1L)
        repeat(1000) {
            val value = random.nextDouble()
            value shouldBeGreaterThanOrEqual 0.0
            value shouldBeLessThan 1.0
        }
    }

    "nextDouble(from, until) always falls in the requested range" {
        val random = SimRandom(2L)
        repeat(1000) {
            val value = random.nextDouble(48.0, 56.0)
            value shouldBeGreaterThanOrEqual 48.0
            value shouldBeLessThan 56.0
        }
    }

    "nextInt(until) always falls in [0, until)" {
        val random = SimRandom(3L)
        repeat(1000) {
            val value = random.nextInt(10)
            (value in 0 until 10) shouldBe true
        }
    }

    "chance(0.0) is never true and chance(1.0) is always true" {
        val random = SimRandom(4L)
        repeat(100) {
            random.chance(0.0) shouldBe false
            random.chance(1.0) shouldBe true
        }
    }

    "nextLong() is deterministic for a given seed" {
        checkAll(Arb.long()) { seed ->
            val a = SimRandom(seed)
            val b = SimRandom(seed)

            List(50) { a.nextLong() } shouldBe List(50) { b.nextLong() }
        }
    }

    "chance(p) comes true roughly p of the time over many rolls" {
        checkAll(iterations = 20, genA = Arb.double(0.05, 0.95)) { probability ->
            val random = SimRandom(5L)
            val trials = 5000
            val hits = (1..trials).count { random.chance(probability) }
            val observedRate = hits.toDouble() / trials

            // Generous tolerance - this is a statistical property, not exact equality.
            kotlin.math.abs(observedRate - probability) shouldBeLessThan 0.05
        }
    }
})
