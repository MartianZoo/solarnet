plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("solarnet.kotlin-base")
}

val webSourceDirectory = rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/web")

kotlin {
  js {
    browser { commonWebpackConfig { cssSupport { enabled.set(true) } } }
    binaries.executable()
  }

  sourceSets {
    jsMain {
      kotlin.setSrcDirs(listOf(webSourceDirectory))
      dependencies {
        implementation(project(":script"))
        implementation(npm("jquery", "3.7.1"))
        implementation(npm("jquery.terminal", "2.46.1"))
        implementation(devNpm("tslib", "2.8.1"))
      }
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/web"))
      )
      dependencies { implementation(kotlin("test")) }
    }
  }
}

tasks.named<ProcessResources>("jsProcessResources") {
  from(webSourceDirectory) { include("*.html", "*.css") }
}
