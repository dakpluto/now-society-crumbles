package com.dakpluto.society.ui

import com.dakpluto.society.engine.terrain.TerrainGenerator
import com.dakpluto.society.ui.map.SubCellBlendPrototype
import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage

// Placeholder until a setup screen exists to supply a real SimulationConfig -
// see CONCEPT.md "Setup / Configuration".
private const val DEMO_SEED = 1234L
private const val DEMO_WIDTH_CELLS = 32
private const val DEMO_HEIGHT_CELLS = 24
private const val DEMO_PIXELS_PER_CELL = 16.0
private const val DEMO_CELLS_PER_TEXTURE_TILE = 2.0

// Temporary comparison wiring for the sub-cell blending prototype (see
// SubCellBlendPrototype) - not the permanent app layout.
class App : Application() {
    override fun start(stage: Stage) {
        val field = TerrainGenerator.generateField(seed = DEMO_SEED)

        val hardEdge = SubCellBlendPrototype.render(
            field, DEMO_WIDTH_CELLS, DEMO_HEIGHT_CELLS, DEMO_PIXELS_PER_CELL, DEMO_CELLS_PER_TEXTURE_TILE,
            sampleGranularityPx = DEMO_PIXELS_PER_CELL,
        )
        val smooth = SubCellBlendPrototype.render(
            field, DEMO_WIDTH_CELLS, DEMO_HEIGHT_CELLS, DEMO_PIXELS_PER_CELL, DEMO_CELLS_PER_TEXTURE_TILE,
        )

        val root = HBox(
            16.0,
            VBox(6.0, Label("Hard edge (one sample per cell)"), ImageView(hardEdge)).apply { alignment = Pos.CENTER },
            VBox(6.0, Label("Smooth (continuous sub-cell sampling)"), ImageView(smooth)).apply { alignment = Pos.CENTER },
        )
        root.alignment = Pos.CENTER
        root.style = "-fx-background-color: white; -fx-padding: 16;"

        stage.title = "Now Society Crumbles"
        stage.scene = Scene(root)
        stage.show()
    }
}

fun main(args: Array<String>) {
    System.setProperty("prism.order", "sw")
    Application.launch(App::class.java, *args)
}
