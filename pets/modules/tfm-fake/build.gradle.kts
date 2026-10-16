import dev.martianzoo.buildlogic.GenerateCatalogSources
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins { id("pets.kmp-jvm-js") }

val generateFakeCanonSources by
    tasks.registering(GenerateCatalogSources::class) {
      inputDirectory.set(
          rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/fake/bundles")
      )
      logicalPrefix.set("bundles")
      packageName.set("dev.martianzoo.tfm.fake")
      sourceName.set("FakeCanon")
      outputDirectory.set(layout.buildDirectory.dir("generated/fakeCanonSources"))
    }

kotlin {
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser()
    nodejs()
  }

  sourceSets {
    commonMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/fake"))
      )
      kotlin.srcDir(generateFakeCanonSources)
      dependencies {
        implementation(project(":catalog"))
        implementation(project(":pets"))
        implementation(project(":tfm-canon"))
      }
    }
    commonTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/fake"))
      )
    }
  }
}
