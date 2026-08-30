plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/engine"))
      )
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(project(":pets"))
      }
    }
    commonTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/engine"))
      )
      dependencies {
        implementation(libs.kotest.assertions.core)
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-engine"))
      }
    }
    jsMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/engine"))
      )
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/engine"))
      )
    }
    jvmMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/engine"))
      )
    }
    jvmTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/engine"))
      )
    }
  }
}
