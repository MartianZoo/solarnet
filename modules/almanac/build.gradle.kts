plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("solarnet.kotlin-base")
}

val sourceDirectory =
    rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/tfm/web/classviewer")

kotlin {
  js {
    browser { commonWebpackConfig { cssSupport { enabled.set(true) } } }
    binaries.executable()
  }

  sourceSets {
    jsMain {
      kotlin.setSrcDirs(listOf(sourceDirectory))
      dependencies {
        implementation(project(":pets"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-text"))
        implementation(devNpm("tslib", "2.8.1"))
      }
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/web/classviewer")
          )
      )
      dependencies { implementation(kotlin("test")) }
    }
  }
}

tasks.register("test") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs the Almanac browser test."
  dependsOn("jsBrowserTest")
}

tasks.named<ProcessResources>("jsProcessResources") {
  from(sourceDirectory) { include("*.html", "*.css") }
}
