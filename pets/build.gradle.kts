import java.net.URL

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.0"
  id("org.jetbrains.dokka") version "1.7.10"
}

kotlin { jvmToolchain(18) }

// kotlin {
//  js {
//    binaries.executable()
//    browser {
//      commonWebpackConfig {
//      }
//    }
//  }
// }

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")

  implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")
  implementation("com.squareup.moshi:moshi-kotlin:1.14.0")

  // TODO will eventually deguavafy
  testImplementation("com.google.guava:guava:31.1-jre")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
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
      samples.from("src/main/kotlin/dev/martianzoo/tfm/pets/samples.kt")
    }
  }
}
