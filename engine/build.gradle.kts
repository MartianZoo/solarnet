plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(project(":pets"))
      }
    }
    commonTest { dependencies { implementation(libs.kotest.assertions.core) } }
  }
}
