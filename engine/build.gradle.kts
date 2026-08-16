plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(project(":pets"))
      }
    }
    commonTest {
      dependencies {
        implementation(libs.kotest.assertions.core)
        implementation(project(":canon")) // easiest to test the engine this way
      }
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
// filter and runs every shared engine test in the browser.
tasks.named<org.gradle.api.tasks.testing.AbstractTestTask>("jsBrowserTest") {
  if (!fullBrowserTestsRequested) {
    filter.includeTestsMatching(
        "dev.martianzoo.tfm.engine.games.Game20260619Test.gameThroughGeneration5"
    )
  }
}

tasks.register("jsBrowserSmokeTest") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs one representative multi-generation game in a browser."
  dependsOn("jsBrowserTest")
}

tasks.register("allTestsIncludingBrowser") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs every engine test on the JVM and in a browser."
  dependsOn("allTests", "jsBrowserTest")
}
