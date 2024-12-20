import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URL

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.9.21"
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("org.jetbrains.dokka") version "1.7.10"
  `java-library`
}

kotlin { jvmToolchain(11) }

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.21"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.21")

  implementation("org.jline:jline:3.21.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
  testImplementation("com.google.truth:truth:1.1.3")

  implementation(project(":pets"))
  implementation(project(":engine"))
  implementation(project(":canon"))

  testImplementation(project(":canon"))
  testImplementation(project(":pets"))
}

tasks.dokkaHtml.configure {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URL("https://github.com/MartianZoo/solarnet/tree/main/repl/src"))
        remoteLineSuffix.set("#L")
      }
      samples.from("src/main/java/dev/martianzoo/repl/samples.kt")
    }
  }
}

tasks {
  named<ShadowJar>("shadowJar") {
    mergeServiceFiles()
    manifest { attributes(mapOf("Main-Class" to "dev.martianzoo.repl.ReplSessionKt")) }
  }
}
