plugins {
  id("solarnet.kmp-jvm-js")
}

val includeSlowTests = providers.gradleProperty("includeSlowTests").orNull?.toBoolean() == true

val requestedTaskNames = gradle.startParameter.taskNames
val slowTestsExplicitlyRequested = requestedTaskNames.any {
  it == "jsBrowserTest" ||
      it.endsWith(":jsBrowserTest") ||
      it == "allTestsIncludingSlow" ||
      it.endsWith(":allTestsIncludingSlow")
}

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

tasks.named("jsBrowserTest") {
  enabled = includeSlowTests || slowTestsExplicitlyRequested
}

tasks.register("allTestsIncludingSlow") {
  group = "verification"
  description = "Runs all engine tests, including slow browser tests."
  dependsOn("allTests")
  dependsOn("jsBrowserTest")
}

dokka {
  dokkaSourceSets {
    named("commonMain") {
      samples.from("src/commonMain/kotlin/dev/martianzoo/tfm/engine/samples.kt")
    }
  }
}
