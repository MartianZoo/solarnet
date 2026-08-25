plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonTest {
      dependencies {
        implementation(libs.kotest.assertions.core)
        implementation(project(":engine"))
        implementation(project(":pets"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-engine"))
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
