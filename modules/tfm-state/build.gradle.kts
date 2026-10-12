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
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/state"))
      )
      dependencies {
        implementation(project(":catalog"))
        implementation(project(":pets"))
        implementation(project(":state"))
        implementation(project(":tfm-canon"))
      }
    }
  }
}
