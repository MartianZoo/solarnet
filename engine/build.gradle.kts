import java.net.URL

plugins {
  id("org.jetbrains.kotlin.jvm") version "2.1.20"
  id("org.jetbrains.dokka") version "1.9.20"
  kotlin("kapt")
  `java-library`
}

kotlin { jvmToolchain(21) }

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.1.20"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.20")
  implementation("com.google.dagger:dagger:2.55")
  kapt("com.google.dagger:dagger-compiler:2.55")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
  testImplementation("com.google.truth:truth:1.1.3")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
  kaptTest("com.google.dagger:dagger-compiler:2.55")

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
