package com.dakpluto.society.ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class App : Application() {
    override fun start(stage: Stage) {
        val root = StackPane(Label("Now Society Crumbles"))
        stage.title = "Now Society Crumbles"
        stage.scene = Scene(root, 800.0, 600.0)
        stage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}
