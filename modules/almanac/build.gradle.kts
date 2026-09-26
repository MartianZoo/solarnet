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
        implementation(devNpm("tslib", "2.8.1"))
      }
    }
  }
}

tasks.named<ProcessResources>("jsProcessResources") {
  from(sourceDirectory) { include("*.html", "*.css") }
}
