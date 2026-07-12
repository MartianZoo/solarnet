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

kotlin {
  jvm()
  js(IR) {
    browser()
  }

  sourceSets {
    commonMain {
      kotlin.srcDir("src/main/java")
      dependencies {
        implementation(project(":pets"))
      }
    }
    commonTest {
      kotlin.srcDir("src/test/java")
      dependencies {
        implementation(kotlin("test"))
        implementation("io.kotest:kotest-assertions-core:6.1.11")
      }
    }
  }
}

tasks.named("jsBrowserTest") {
  dependsOn(copyCanonResourcesForKarma)
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
      samples.from("src/main/java/dev/martianzoo/tfm/canon/samples.kt")
    }
  }
}
