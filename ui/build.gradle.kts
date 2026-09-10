plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.javafx)
    alias(libs.plugins.runtime)
    application
}

kotlin {
    jvmToolchain(25)
}

javafx {
    version = "25"
    modules("javafx.controls")
}

dependencies {
    implementation(project(":engine"))

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

application {
    mainClass.set("com.dakpluto.society.ui.AppKt")
}

// runtime{} (org.beryx.runtime -> jlink/jpackage) module list and jpackage image/
// installer config are deferred until there's enough real UI code to run
// `./gradlew suggestModules` against, per CONCEPT.md's "lock the structure, defer
// the exact numbers" pattern used throughout the design doc.

tasks.test {
    useJUnitPlatform()
}
