package com.dakpluto.society.engine.disaster

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DisasterTypeTest : StringSpec({
    "the roster has 18 entries: 15 base types plus 3 Epidemic variants" {
        DisasterType.entries.size shouldBe 18
    }

    "Sandstorm is the only fully unclassified type - a documented CONCEPT.md gap" {
        DisasterType.UNCLASSIFIED_TYPES shouldBe setOf(DisasterType.SANDSTORM)
    }

    "Tsunami is only partially classified: Radial falloff only, per its documented composite gap" {
        DisasterType.TSUNAMI.spreadPatterns shouldBe setOf(SpreadPattern.RADIAL_FALLOFF)
    }

    "Volcanic eruption is the only composite (Gradient flow + Directional track)" {
        DisasterType.COMPOSITE_TYPES shouldBe setOf(DisasterType.VOLCANIC_ERUPTION)
        DisasterType.VOLCANIC_ERUPTION.spreadPatterns shouldBe setOf(
            SpreadPattern.GRADIENT_FLOW,
            SpreadPattern.DIRECTIONAL_TRACK,
        )
    }

    "all three Epidemic variants share the Contact/connectivity-driven pattern" {
        val epidemics = setOf(DisasterType.EPIDEMIC_CROP, DisasterType.EPIDEMIC_LIVESTOCK, DisasterType.EPIDEMIC_HUMAN)

        for (epidemic in epidemics) {
            epidemic.spreadPatterns shouldBe setOf(SpreadPattern.CONTACT_CONNECTIVITY_DRIVEN)
        }
    }

    "Gradient flow covers Avalanche, Flood, Landslide, and Volcanic eruption" {
        val gradientFlowTypes = DisasterType.entries.filter { SpreadPattern.GRADIENT_FLOW in it.spreadPatterns }.toSet()

        gradientFlowTypes shouldBe setOf(
            DisasterType.AVALANCHE,
            DisasterType.FLOOD,
            DisasterType.LANDSLIDE,
            DisasterType.VOLCANIC_ERUPTION,
        )
    }
})
