import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URI

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("org.jetbrains.dokka")
  `java-library`
}

dependencies {
  implementation(project(":repl"))
  implementation("org.jline:jline:3.21.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
  testImplementation("com.google.truth:truth:1.1.3")
}

tasks.dokkaHtml.configure {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URI("https://github.com/MartianZoo/solarnet/tree/main/interactive/src").toURL())
        remoteLineSuffix.set("#L")
      }
    }
  }
}

tasks {
  named<ShadowJar>("shadowJar") {
    mergeServiceFiles()
    manifest { attributes(mapOf("Main-Class" to "dev.martianzoo.interactive.JlineReplKt")) }
  }
}
