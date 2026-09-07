import dev.martianzoo.buildlogic.GenerateCatalogSources

plugins { id("solarnet.kmp-jvm-js") }

val generateFakeCanonSources by
    tasks.registering(GenerateCatalogSources::class) {
      inputDirectory.set(
          rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/fake")
      )
      logicalPrefix.set("bundles")
      packageName.set("dev.martianzoo.tfm.fake")
      sourceName.set("FakeCanon")
      outputDirectory.set(layout.buildDirectory.dir("generated/fakeCanonSources"))
    }

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/fake"))
      )
      kotlin.srcDir(generateFakeCanonSources)
      dependencies {
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
