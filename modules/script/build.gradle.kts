plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/script"),
              rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/script"),
          )
      )
      dependencies {
        implementation(project(":pets"))
        implementation(project(":engine"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-engine"))
      }
    }
    commonTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/script"))
      )
    }
    jsMain {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/script"),
              rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/script"),
          )
      )
    }
    jvmMain {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/script"),
              rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tfm/script"),
          )
      )
    }
  }
}
