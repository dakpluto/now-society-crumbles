package com.dakpluto.society.ui

import com.dakpluto.society.engine.terrain.TerrainGenerator
import com.dakpluto.society.ui.map.SubCellBlendPrototype
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.stage.Stage

// Placeholder until a setup screen exists to supply a real SimulationConfig -
// see CONCEPT.md "Setup / Configuration".
private const val DEMO_SEED = 1234L
private const val DEMO_WIDTH_CELLS = 48
private const val DEMO_HEIGHT_CELLS = 36
private const val DEMO_PIXELS_PER_CELL = 16.0
private const val DEMO_CELLS_PER_TEXTURE_TILE = 2.0

class App : Application() {
    override fun start(stage: Stage) {
        val field = TerrainGenerator.generateField(seed = DEMO_SEED)
        val map = SubCellBlendPrototype.render(field, DEMO_WIDTH_CELLS, DEMO_HEIGHT_CELLS, DEMO_PIXELS_PER_CELL, DEMO_CELLS_PER_TEXTURE_TILE)

        val root = StackPane(ImageView(map))
        stage.title = "Now Society Crumbles"
        stage.scene = Scene(root, map.width, map.height)
        stage.show()
    }
}

fun main(args: Array<String>) {
    // JavaFX's hardware-accelerated pipeline rendered a completely blank
    // window (no error) in this dev environment - software rendering fixed
    // it. Keep this until that's root-caused, since a silent blank window
    // is a much worse failure mode than a slower render.
    System.setProperty("prism.order", "sw")
    Application.launch(App::class.java, *args)
}
