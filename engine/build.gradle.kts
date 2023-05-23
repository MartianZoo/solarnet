import java.net.URL

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.0"
  id("org.jetbrains.dokka") version "1.7.10"
  kotlin("kapt")
  `java-library`
}

kotlin { jvmToolchain(11) }

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")
  implementation("com.google.dagger:dagger:2.46.1")
  kapt("com.google.dagger:dagger-compiler:2.46.1")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
  testImplementation(project(mapOf("path" to ":repl")))
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
  testImplementation("com.google.truth:truth:1.1.3")

  implementation(project(":pets"))

  testImplementation(project(":canon")) // easiest to test the engine this way
  testImplementation(project(":pets")) // eep
}

tasks.dokkaHtml.configure {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URL("https://github.com/MartianZoo/solarnet/tree/main/engine/src"))
        remoteLineSuffix.set("#L")
      }
      samples.from("src/main/java/dev/martianzoo/tfm/engine/samples.kt")
    }
  }
}
