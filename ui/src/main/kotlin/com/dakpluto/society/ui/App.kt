package com.dakpluto.society.ui

import com.dakpluto.society.engine.config.MapConfig
import com.dakpluto.society.engine.terrain.TerrainGenerator
import com.dakpluto.society.ui.map.MapView
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage

// Placeholder until a setup screen exists to supply a real SimulationConfig -
// see CONCEPT.md "Setup / Configuration".
private const val DEMO_SEED = 1234L
private val DEMO_MAP = MapConfig(widthCells = 64, heightCells = 48)
private const val DEMO_CELL_SIZE_PX = 12.0

class App : Application() {
    override fun start(stage: Stage) {
        val grid = TerrainGenerator.generate(seed = DEMO_SEED, map = DEMO_MAP)
        val mapView = MapView(DEMO_CELL_SIZE_PX).apply { render(grid) }

        val root = StackPane(mapView)
        stage.title = "Now Society Crumbles"
        stage.scene = Scene(root, mapView.width, mapView.height)
        stage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}
