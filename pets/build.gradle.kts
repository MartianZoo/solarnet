plugins {
  id("solarnet.kmp-jvm-js")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        // Pin the exact tested JitPack build from the better-parse fork; tag lookup was unreliable.
        implementation(libs.better.parse)
      }
    }
    commonTest { dependencies { implementation(libs.kotest.assertions.core) } }
  }
}
