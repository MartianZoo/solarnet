import java.net.URL

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.dokka")
}

dependencies {
  implementation("io.insert-koin:koin-core:3.5.6")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
  testImplementation("com.google.truth:truth:1.1.3")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")

  implementation(project(":pets"))

  testImplementation(project(":canon")) // easiest to test the engine this way
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
