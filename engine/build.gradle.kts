import java.net.URL

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.dokka")
  id("com.google.devtools.ksp") version "2.1.20-1.0.32"
  `java-library`
}

kotlin { jvmToolchain(21) }

dependencies {
  implementation("com.google.dagger:dagger:2.55")
  ksp("com.google.dagger:dagger-compiler:2.55")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
  testImplementation("com.google.truth:truth:1.1.3")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
  kspTest("com.google.dagger:dagger-compiler:2.55")

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
