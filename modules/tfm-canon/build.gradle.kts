import dev.martianzoo.buildlogic.GenerateCatalogSources

plugins {
  id("solarnet.kmp-jvm-js")
  alias(libs.plugins.kotlin.serialization)
}

val generateCanonSources by
    tasks.registering(GenerateCatalogSources::class) {
      inputDirectory.set(
          rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/canon")
      )
      logicalPrefix.set("bundles")
      packageName.set("dev.martianzoo.tfm.canon")
      sourceName.set("Canon")
      outputDirectory.set(layout.buildDirectory.dir("generated/canonSources"))
    }

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/canon"))
      )
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
