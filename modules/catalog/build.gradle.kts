import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser()
    nodejs()
  }

  sourceSets {
    commonMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/catalog"))
      )
      dependencies { implementation(project(":pets")) }
    }
    commonTest {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/catalog"),
              rootProject.layout.projectDirectory.dir(
                  "test/common/dev/martianzoo/catalogtestsupport"
              ),
          )
      )
      dependencies { implementation(libs.kotest.assertions.core) }
    }
  }
}
