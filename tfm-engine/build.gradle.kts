plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(project(":engine"))
        implementation(project(":pets"))
      }
    }
  }
}
