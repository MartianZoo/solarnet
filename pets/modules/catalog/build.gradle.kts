plugins { id("pets.kmp-jvm-js") }

kotlin {
  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class) wasmJs { browser() }
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
