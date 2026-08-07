plugins {
  id("solarnet.kmp-jvm-js")
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // Pin the exact tested JitPack build from the better-parse fork; tag lookup was unreliable.
        implementation(libs.better.parse)
        implementation(libs.kotlinx.serialization.json)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.kotest.assertions.core)
        implementation(project(":canon")) // easier to test the pets data model this way
      }
    }
  }
}
