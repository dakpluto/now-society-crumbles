package com.dakpluto.society.engine.config

import kotlinx.serialization.Serializable

/**
 * The shareable config/seed file: resolved input parameters only, not a generated
 * snapshot. Loading a config re-runs the same deterministic procedural generation
 * from [seed] plus these inputs. See CONCEPT.md -> "Shareable config/seed file schema".
 */
@Serializable
data class SimulationConfig(
    val schemaVersion: Int = 1,
    val seed: Long,
    val complexityLevel: Int,
    val societySize: Int,
    // TODO: replace with a finalized Era enum once the starting-era list is locked
    //  (see CONCEPT.md Open Questions).
    val startingEra: String,
    val environment: EnvironmentConfig,
    // TODO: key by a finalized Stat enum once the master stat list settles
    //  (see CONCEPT.md -> Stat System -> "Master stat list (draft)").
    val startingTraits: Map<String, Double>,
    val uniqueUnitCount: Int,
    val map: MapConfig,
)

@Serializable
data class EnvironmentConfig(
    // Display metadata only; the simulation reads terrainElements, not this label.
    // Null if fully custom (complexity tier 10).
    val terrainArchetype: String?,
    // TODO: key by a finalized TerrainElement enum once terrain composition weights
    //  are defined (see CONCEPT.md -> "Terrain composition elements").
    val terrainElements: Map<String, Double>,
    val waterResources: Double,
    val landQuality: Double,
    val wildlife: Double,
    val weatherPatterns: Double,
)

@Serializable
data class MapConfig(
    val widthCells: Int,
    val heightCells: Int,
)
