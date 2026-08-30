plugins { id("solarnet.kmp-jvm-js") }

val otbGame20260818Replay =
    rootProject.layout.projectDirectory.file(
        "test/common/dev/martianzoo/tfm/tests/replays/OtbGame20260818-world-export.rego"
    )
val generatedReplaySources =
    layout.buildDirectory.dir("generated/sources/replays/commonTest/kotlin")
val generateReplaySources by tasks.registering {
  inputs.file(otbGame20260818Replay)
  outputs.dir(generatedReplaySources)
  doLast {
    val replay = inputs.files.singleFile.readText()
    val delimiter = "\"\"\""
    require(delimiter !in replay) { "REgo replay cannot contain a Kotlin raw-string delimiter" }
    val output =
        outputs.files.singleFile.resolve(
            "dev/martianzoo/tfm/tests/replays/OtbGame20260818Replay.kt"
        )
    output.parentFile.mkdirs()
    output.writeText(
        "package dev.martianzoo.tfm.tests.replays\n\n" +
            "internal val otbGame20260818Replay: String =\n" +
            "    $delimiter\n" +
            replay +
            "$delimiter.trimIndent()\n"
    )
  }
}

kotlin {
  sourceSets {
    commonTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/tests"))
      )
      kotlin.srcDir(generateReplaySources)
      dependencies {
        implementation(libs.kotest.assertions.core)
        implementation(project(":engine"))
        implementation(project(":pets"))
        implementation(project(":script"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-engine"))
      }
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/tests"))
      )
    }
    jvmTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tfm/tests"))
      )
    }
  }
}

val fullBrowserTestsRequested =
    providers.gradleProperty("includeBrowserTests").orNull?.toBoolean() == true ||
        gradle.startParameter.taskNames.any {
          it.substringAfterLast(':') in setOf("jsBrowserTest", "allTestsIncludingBrowser")
        }

// A routine build exercises one representative multi-generation game in Chrome. Naming the full
// browser task directly, using allTestsIncludingBrowser, or setting includeBrowserTests removes the
// filter and runs every shared Terraforming Mars test in the browser.
tasks.named<org.gradle.api.tasks.testing.AbstractTestTask>("jsBrowserTest") {
  if (!fullBrowserTestsRequested) {
    filter.includeTestsMatching(
        "dev.martianzoo.tfm.tests.replays.Game20260619Test.gameThroughGeneration5"
    )
  }
}

tasks.register("jsBrowserSmokeTest") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs one representative multi-generation Terraforming Mars game in a browser."
  dependsOn("jsBrowserTest")
}

tasks.register("allTestsIncludingBrowser") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs every Terraforming Mars test on both the JVM and browser."
  dependsOn("allTests", "jsBrowserTest")
}
