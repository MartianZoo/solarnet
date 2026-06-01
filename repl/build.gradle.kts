import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URL

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("org.jetbrains.dokka")
  `java-library`
}

dependencies {
  implementation("org.jline:jline:3.21.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
  testImplementation("com.google.truth:truth:1.1.3")

  implementation(project(":pets"))
  implementation(project(":engine"))
  implementation(project(":canon"))

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
