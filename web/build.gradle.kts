plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("solarnet.kotlin-base")
}

kotlin {
  js {
    browser { commonWebpackConfig { cssSupport { enabled.set(true) } } }
    binaries.executable()
  }

  sourceSets {
    jsMain {
      dependencies {
        implementation(project(":script"))
        implementation(npm("jquery", "3.7.1"))
        implementation(npm("jquery.terminal", "2.46.1"))
        implementation(devNpm("tslib", "2.8.1"))
      }
    }
    jsTest { dependencies { implementation(kotlin("test")) } }
  }
}
