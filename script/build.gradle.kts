plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":pets"))
        implementation(project(":engine"))
        implementation(project(":tfm-canon"))
      }
    }
  }
}
