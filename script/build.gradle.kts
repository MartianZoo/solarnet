import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URI

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.gradleup.shadow") version "9.2.2"
  id("org.jetbrains.dokka")
  `java-library`
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.14.4"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("com.google.truth:truth:1.4.5")

  implementation(project(":pets"))
  implementation(project(":engine"))
  implementation(project(":canon"))
}

dokka {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URI("https://github.com/MartianZoo/solarnet/tree/main/script/src"))
        remoteLineSuffix.set("#L")
      }
    }
    named("main") {
      samples.from("src/main/kotlin/dev/martianzoo/script/samples.kt")
    }
  }
}

tasks {
  named<ShadowJar>("shadowJar") {
    mergeServiceFiles()
    manifest { attributes(mapOf("Main-Class" to "dev.martianzoo.script.ScriptSessionKt")) }
  }
}
