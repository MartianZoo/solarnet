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

// The engine's browser suite is slow, so a routine build skips it. It runs when `includeSlowTests`
// is set, or when it is named on the command line either directly or through the task below.
val slowTestTaskNames = setOf("jsBrowserTest", "allTestsIncludingSlow")
val runSlowTests =
    providers.gradleProperty("includeSlowTests").orNull?.toBoolean() == true ||
        gradle.startParameter.taskNames.any { it.substringAfterLast(':') in slowTestTaskNames }

tasks.named("jsBrowserTest") { enabled = runSlowTests }

tasks.register("allTestsIncludingSlow") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs all engine tests, including slow browser tests."
  dependsOn("allTests", "jsBrowserTest")
}
