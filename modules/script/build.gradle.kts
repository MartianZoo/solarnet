plugins { id("solarnet.kmp-jvm-js") }

kotlin {
  sourceSets {
    commonMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/script"))
      )
      dependencies {
        implementation(project(":agent"))
        implementation(project(":catalog"))
        implementation(project(":pets"))
        implementation(project(":engine"))
        implementation(project(":state"))
        implementation(project(":tfm-canon"))
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
