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
        implementation(project(":tfm-canon"))
        implementation(project(":engine"))
        implementation(project(":pets"))
        implementation(project(":tfm-engine"))
        implementation(libs.kotlinx.serialization.json)
      }
    }
    jsTest { dependencies { implementation(kotlin("test")) } }
  }
}
