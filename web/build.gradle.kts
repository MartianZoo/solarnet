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

// The served app reads Canon and Pets data at runtime, so fold their resources into this module's
// own resource processing; everything downstream of it then picks them up automatically.
tasks.named<ProcessResources>("jsProcessResources") {
  dependsOn(":tfm-canon:jsProcessResources", ":pets:jsProcessResources")
  from(project(":tfm-canon").layout.buildDirectory.dir("processedResources/js/main"))
  from(project(":pets").layout.buildDirectory.dir("processedResources/js/main/pets")) {
    into("pets")
  }
}
