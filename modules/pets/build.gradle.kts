plugins { id("solarnet.kmp-jvm-js") }

val commonSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/pets")
val commonTestDirectories =
    listOf(
        rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/pets"),
        rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/pets"),
        rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/testlib"),
    )

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(listOf(commonSourceDirectory))
      dependencies {
        // Pin the exact tested JitPack build from the better-parse fork; tag lookup was unreliable.
        implementation(libs.better.parse)
      }
    }
    commonTest {
      kotlin.setSrcDirs(commonTestDirectories)
      dependencies { implementation(libs.kotest.assertions.core) }
    }
    jsMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/pets"))
      )
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/pets"))
      )
    }
    jvmMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/pets"))
      )
    }
    jvmTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/pets"))
      )
    }
  }
}
