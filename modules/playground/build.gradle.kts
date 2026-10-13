@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("solarnet.kotlin-base")
}

val sourceDirectory =
    rootProject.layout.projectDirectory.dir("src/wasmJs/dev/martianzoo/tfm/web/playground")
val localDirectory = rootProject.layout.projectDirectory.dir("_local")
val replayEventLogsDirectory =
    project(":tfm-tests").layout.buildDirectory.dir("generated/replay-event-logs")

kotlin {
  wasmJs {
    outputModuleName.set("playground")
    browser { commonWebpackConfig { outputFileName = "playground.js" } }
    binaries.executable()
  }

  sourceSets {
    wasmJsMain {
      // Adds rather than replaces: Compose generates resource accessors into this source set.
      kotlin.srcDir(sourceDirectory)
      dependencies {
        implementation(project(":catalog"))
        implementation(project(":pets"))
        implementation(project(":state"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-fake"))
        implementation(project(":tfm-state"))
        val compose = libs.versions.compose.multiplatform.get()
        implementation("org.jetbrains.compose.runtime:runtime:$compose")
        implementation("org.jetbrains.compose.foundation:foundation:$compose")
        implementation("org.jetbrains.compose.material:material:$compose")
        implementation("org.jetbrains.compose.ui:ui:$compose")
        implementation("org.jetbrains.compose.components:components-resources:$compose")
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}

val bundleResources =
    tasks.register<Sync>("bundleResources") {
      description = "Packages replay recordings and local game art for the history viewer."
      dependsOn(":tfm-tests:jvmTest")
      from(layout.projectDirectory.dir("resources")) { exclude("files/specimen.json") }
      from(replayEventLogsDirectory) {
        include("*.json")
        exclude("*.short.json")
        into("files/games")
      }
      from(localDirectory.dir("images")) {
        include("*.png")
        into("files/cards")
      }
      from(localDirectory.dir("images/30px")) {
        include("*.png", "MC/*.png")
        into("files/icons")
      }
      from(
          rootProject.layout.projectDirectory.file(
              "src/js/dev/martianzoo/tfm/web/shared/map/mars.png"
          )
      ) {
        into("files/map")
      }
      into(layout.buildDirectory.dir("bundled-resources"))
      doLast {
        val gamesDirectory = destinationDir.resolve("files/games").also { it.mkdirs() }
        val names =
            gamesDirectory
                .listFiles { file -> file.isFile && file.extension == "json" }
                .orEmpty()
                .map { it.nameWithoutExtension }
                .sorted()
        gamesDirectory.resolve("index.txt").writeText(names.joinToString("\n"))
      }
    }

compose.resources {
  packageOfResClass = "dev.martianzoo.tfm.web.playground.resources"
  generateResClass = always
  customDirectory(
      sourceSetName = "wasmJsMain",
      directoryProvider =
          bundleResources.map { layout.buildDirectory.dir("bundled-resources").get() },
  )
}

tasks.named<ProcessResources>("wasmJsProcessResources") {
  from(sourceDirectory) { include("*.html") }
}

// The development server's layout endpoint (webpack.config.d/layout-endpoint.js) reads and writes
// this file, so window arrangements survive restarts and can be edited by hand.
tasks.named<KotlinWebpack>("wasmJsBrowserDevelopmentRun") {
  args.add("--env")
  args.add("playgroundLayout=${localDirectory.file("playground/layout.json").asFile.path}")
}
