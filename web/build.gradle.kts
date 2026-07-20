import java.net.URI

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.dokka")
}

kotlin {
  js(IR) {
    browser {
      commonWebpackConfig {
        cssSupport { enabled.set(true) }
      }
    }
    binaries.executable()
  }

  sourceSets {
    jsMain {
      dependencies {
        implementation(project(":script"))
        implementation(npm("jquery", "3.7.1"))
        implementation(npm("jquery.terminal", "2.46.1"))
      }
    }
    jsTest {
      dependencies { implementation(kotlin("test")) }
    }
  }
}

val collectRuntimeResources by
    tasks.registering(Copy::class) {
      dependsOn("jsProcessResources")
      dependsOn(":canon:jsProcessResources")
      dependsOn(":pets:jsProcessResources")
      from(project(":canon").layout.buildDirectory.dir("processedResources/js/main"))
      from(project(":pets").layout.buildDirectory.dir("processedResources/js/main/pets")) {
        into("pets")
      }
      into(layout.buildDirectory.dir("processedResources/js/main"))
    }

tasks
    .matching { it.name.startsWith("jsBrowser") && it.name != "jsBrowserTest" }
    .configureEach { dependsOn(collectRuntimeResources) }

tasks
    .matching { it.name.startsWith("compile") && it.name.endsWith("KotlinJs") }
    .configureEach { dependsOn(collectRuntimeResources) }

dokka {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URI("https://github.com/MartianZoo/solarnet/tree/main/web/src"))
        remoteLineSuffix.set("#L")
      }
    }
  }
}
