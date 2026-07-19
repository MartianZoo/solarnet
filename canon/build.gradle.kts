import java.net.URI

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.dokka")
}

val copyCanonResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn("jsProcessResources")
      from(layout.buildDirectory.dir("processedResources/js/main"))
      into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-canon-test"))
    }

val copyPetsResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn(":pets:jsProcessResources")
      from(project(":pets").layout.buildDirectory.dir("processedResources/js/main/pets"))
      into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-canon-test/pets"))
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
      }
    }
    commonTest {
      dependencies {
        implementation(kotlin("test"))
        implementation("io.kotest:kotest-assertions-core:6.1.11")
      }
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
        remoteUrl.set(URI("https://github.com/MartianZoo/solarnet/tree/main/canon/src"))
        remoteLineSuffix.set("#L")
      }
    }
    named("commonMain") {
      samples.from("src/commonMain/kotlin/dev/martianzoo/tfm/canon/samples.kt")
    }
  }
}
