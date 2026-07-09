import java.net.URI

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("org.jetbrains.dokka")
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
        implementation("com.github.kevinb9n.better-parse:better-parse:v0.4.4-2")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
      }
    }
    commonTest {
      dependencies {
        implementation(kotlin("test"))
      }
    }
    jvmTest {
      kotlin.srcDir("src/test/java")
      dependencies {
        implementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
        runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
        implementation("com.google.truth:truth:1.1.3")

        implementation(project(":canon")) // easier to test the engine this way
      }
    }
  }
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
