import java.net.URI

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.dokka")
}

val copyCanonResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn(":canon:jsProcessResources")
      from(project(":canon").layout.buildDirectory.dir("processedResources/js/main"))
      into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-script-test"))
    }

val copyPetsResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn(":pets:jsProcessResources")
      from(project(":pets").layout.buildDirectory.dir("processedResources/js/main/pets"))
      into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-script-test/pets"))
    }

kotlin {
  jvm()
  js(IR) {
    browser()
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":pets"))
        implementation(project(":engine"))
        implementation(project(":canon"))
      }
    }
    commonTest {
      dependencies { implementation(kotlin("test")) }
    }
  }
}

tasks.named("jsBrowserTest") {
  dependsOn(copyCanonResourcesForKarma)
  dependsOn(copyPetsResourcesForKarma)
}

dokka {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URI("https://github.com/MartianZoo/solarnet/tree/main/script/src"))
        remoteLineSuffix.set("#L")
      }
    }
  }
}
