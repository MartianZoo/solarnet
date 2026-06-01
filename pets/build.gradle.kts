import java.net.URL

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.dokka")
}

dependencies {
  implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")
  implementation("com.squareup.moshi:moshi-kotlin:1.14.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
  testImplementation("com.google.truth:truth:1.1.3")

  testImplementation(project(":canon")) // easier to test the engine this way
}

tasks.dokkaHtml.configure {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URL("https://github.com/MartianZoo/solarnet/tree/main/pets/src"))
        remoteLineSuffix.set("#L")
      }
      samples.from("src/main/java/dev/martianzoo/tfm/pets/samples.kt")
    }
  }
}
