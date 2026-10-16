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
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/engine"))
      )
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(project(":agent"))
        implementation(
            "dev.martianzoo.pets:catalog:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(project(":engine"))
        implementation("dev.martianzoo.pets:pets:${providers.gradleProperty("petsVersion").get()}")
        implementation(project(":state"))
        implementation(
            "dev.martianzoo.pets:tfm-canon:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(project(":tfm-state"))
      }
    }
    jsMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/engine"))
      )
    }
    jvmMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tfm/engine"))
      )
    }
  }
}
