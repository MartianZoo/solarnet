import dev.martianzoo.buildlogic.GenerateCatalogSources

plugins {
  id("solarnet.kmp-jvm-js")
  alias(libs.plugins.kotlin.serialization)
}

val generateCardDataSources by
    tasks.registering(GenerateCatalogSources::class) {
      inputDirectory.set(
          rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/canon")
      )
      includedFileNames.set(setOf("cards.json5"))
      logicalPrefix.set("carddata")
      packageName.set("dev.martianzoo.tfm.carddata")
      sourceName.set("CardData")
      outputDirectory.set(layout.buildDirectory.dir("generated/cardDataSources"))
    }

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/carddata"))
      )
      kotlin.srcDir(generateCardDataSources)
      dependencies { implementation(libs.kotlinx.serialization.json) }
    }
  }
}

configurations.configureEach {
  withDependencies {
    require(none { it.name == "pets" }) { "tfm-card-data must not depend on pets" }
  }
}
