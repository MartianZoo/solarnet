plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/state"))
      )
      dependencies {
        implementation(libs.kotlinx.serialization.json)
        implementation(project(":pets"))
      }
    }
    commonTest {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/testsupport"),
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/state"),
          )
      )
      dependencies { implementation(libs.kotest.assertions.core) }
    }
  }
}
