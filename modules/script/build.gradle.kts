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
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/script"))
      )
      dependencies {
        implementation(project(":agent"))
        implementation(
            "dev.martianzoo.pets:catalog:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation("dev.martianzoo.pets:pets:${providers.gradleProperty("petsVersion").get()}")
        implementation(project(":engine"))
        implementation(project(":state"))
        implementation(
            "dev.martianzoo.pets:tfm-canon:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(project(":tfm-engine"))
        implementation(project(":tfm-state"))
      }
    }
    commonTest {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir(
                  "test/common/dev/martianzoo/agenttestsupport"
              ),
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/testsupport"),
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/script"),
          )
      )
    }
  }
}
