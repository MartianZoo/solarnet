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
    browser {
      commonWebpackConfig { outputFileName = "playground.js" }
      testTask { useKarma { useChromeHeadless() } }
    }
    binaries.executable()
  }

  sourceSets {
    wasmJsTest {
      kotlin.srcDir(
          rootProject.layout.projectDirectory.dir("test/wasmJs/dev/martianzoo/tfm/web/playground")
      )
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotlinx.coroutines.test)
      }
    }
    wasmJsMain {
      // Adds rather than replaces: Compose generates resource accessors into this source set.
      kotlin.srcDir(sourceDirectory)
      dependencies {
        implementation(project(":agent"))
        implementation(
            "dev.martianzoo.pets:catalog:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(project(":engine"))
        implementation("dev.martianzoo.pets:pets:${providers.gradleProperty("petsVersion").get()}")
        implementation(project(":state"))
        implementation(
            "dev.martianzoo.pets:tfm-canon:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(project(":tfm-engine"))
        implementation(
            "dev.martianzoo.pets:tfm-fake:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(project(":tfm-state"))
        implementation("dev.martianzoo.pets:text:${providers.gradleProperty("petsVersion").get()}")
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
      description = "Packages replay recordings and fonts for the Playground."
      dependsOn(":tools:buildReplayIndex")
      from(layout.projectDirectory.dir("resources")) { exclude("files/specimen.json") }
      from(replayEventLogsDirectory) {
        include("*.json")
        exclude("*.short.json")
        into("files/games")
      }
      from(project(":tools").layout.buildDirectory.file("generated/replay-index/index.json")) {
        into("files/games")
      }
      into(layout.buildDirectory.dir("bundled-resources"))
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

// The development server writes layout adjustments here for review. The app starts from code
// defaults and never reads this snapshot back.
tasks.named<KotlinWebpack>("wasmJsBrowserDevelopmentRun") {
  args.add("--env")
  args.add("playgroundLayout=${localDirectory.file("playground/layout.json").asFile.path}")
}
