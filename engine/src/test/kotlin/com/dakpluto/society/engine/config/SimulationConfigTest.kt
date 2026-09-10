package com.dakpluto.society.engine.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class SimulationConfigTest : StringSpec({
    "SimulationConfig round-trips through JSON" {
        val config = SimulationConfig(
            seed = 42L,
            complexityLevel = 5,
            societySize = 100,
            startingEra = "BronzeAge",
            environment = EnvironmentConfig(
                terrainArchetype = "Desert",
                terrainElements = mapOf("Sand" to 0.7, "Hills" to 0.2, "Rock" to 0.1),
                waterResources = 0.3,
                landQuality = 0.5,
                wildlife = 0.4,
                weatherPatterns = 0.5,
            ),
            startingTraits = mapOf("Intelligence" to 55.0, "WorkEthic" to 60.0),
            uniqueUnitCount = 20,
            map = MapConfig(widthCells = 64, heightCells = 64),
        )

        val json = Json.encodeToString(SimulationConfig.serializer(), config)
        val decoded = Json.decodeFromString(SimulationConfig.serializer(), json)

        decoded shouldBe config
    }
})
