plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("solarnet.kotlin-base")
}

kotlin {
  js {
    nodejs()
    binaries.executable()
    generateTypeScriptDefinitions()
  }

  sourceSets {
    jsMain {
      dependencies {
        implementation(project(":canon"))
        implementation(project(":engine"))
        implementation(project(":pets"))
        implementation(libs.kotlinx.serialization.json)
      }
    }
    jsTest { dependencies { implementation(kotlin("test")) } }
  }
}

// A parity session reads the same runtime Canon and Pets resources as the browser app.
tasks.named<Copy>("jsProcessResources") {
  dependsOn(":canon:jsProcessResources", ":pets:jsProcessResources")
  from(project(":canon").layout.buildDirectory.dir("processedResources/js/main"))
  from(project(":pets").layout.buildDirectory.dir("processedResources/js/main/pets")) {
    into("pets")
  }
}
