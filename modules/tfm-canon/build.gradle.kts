import dev.martianzoo.buildlogic.GenerateCatalogSources
import org.gradle.api.tasks.Sync

plugins {
  id("solarnet.kmp-jvm-js")
  alias(libs.plugins.kotlin.serialization)
}

val canonSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/canon")
val canonInputsDirectory = layout.buildDirectory.dir("generated/canonInputs")
val generatedCardPetsDirectory =
    project(":tfm-card-generator").layout.buildDirectory.dir("generated/cardPets")

val prepareCanonInputs by
    tasks.registering(Sync::class) {
      dependsOn(":tfm-card-generator:generateCardPets")
      from(canonSourceDirectory) {
        exclude("**/*.kt")
        exclude("*/cards.json5")
        exclude("*/cards.pets")
      }
      from(generatedCardPetsDirectory)
      into(canonInputsDirectory)
    }

val generateCanonSources by
    tasks.registering(GenerateCatalogSources::class) {
      dependsOn(prepareCanonInputs)
      inputDirectory.set(canonInputsDirectory)
      logicalPrefix.set("bundles")
      packageName.set("dev.martianzoo.tfm.canon")
      sourceName.set("Canon")
      outputDirectory.set(layout.buildDirectory.dir("generated/canonSources"))
    }

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(listOf(canonSourceDirectory))
      kotlin.srcDir(generateCanonSources)
      dependencies {
        implementation(libs.kotlinx.serialization.json)
        implementation(project(":pets"))
      }
    }
    commonTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/canon"))
      )
      dependencies { implementation(libs.kotest.assertions.core) }
    }
    jsMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/canon"))
      )
    }
    jvmMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tfm/canon"))
      )
    }
  }
}
