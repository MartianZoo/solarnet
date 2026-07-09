import java.net.URI

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("org.jetbrains.dokka")
}

val copyCanonResourcesForKarma by tasks.registering(Copy::class) {
  dependsOn(":canon:jsProcessResources")
  from(project(":canon").layout.buildDirectory.dir("processedResources/js/main"))
  into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-pets-test"))
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
        implementation("com.github.kevinb9n.better-parse:better-parse:1e227e760395f6dca1cfd00de4d499c7229c51e3")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
      }
    }
    commonTest {
      kotlin.srcDir("src/test/java")
      dependencies {
        implementation(kotlin("test"))
        implementation("io.kotest:kotest-assertions-core:5.9.1")
        implementation(project(":canon")) // easier to test the pets data model this way
      }
    }
    jvmTest {
    }
  }
}

tasks.named("jsBrowserTest") {
  dependsOn(copyCanonResourcesForKarma)
}

tasks.dokkaHtml.configure {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URI("https://github.com/MartianZoo/solarnet/tree/main/pets/src").toURL())
        remoteLineSuffix.set("#L")
      }
      samples.from("src/main/java/dev/martianzoo/tfm/pets/samples.kt")
    }
  }
}
