import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URI
import org.gradle.api.tasks.testing.Test

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.gradleup.shadow") version "9.2.2"
  id("org.jetbrains.dokka")
  `java-library`
}

dependencies {
  implementation(project(":script"))
  implementation("org.jline:jline:3.30.15")

  testImplementation(platform("org.junit:junit-bom:5.14.4"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("com.google.truth:truth:1.4.5")
}

dokka {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URI("https://github.com/MartianZoo/solarnet/tree/main/repl/src"))
        remoteLineSuffix.set("#L")
      }
    }
  }
}

tasks {
  named<ShadowJar>("shadowJar") {
    mergeServiceFiles()
    manifest { attributes(mapOf("Main-Class" to "dev.martianzoo.repl.JlineReplKt")) }
  }

  test {
    filter { excludeTestsMatching("dev.martianzoo.repl.JlineReplSmokeTest") }
  }

  register<Test>("realTerminalSmokeTest") {
    description = "Runs the REPL smoke test in a real terminal session using Expect."
    group = "verification"
    dependsOn(shadowJar)
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching("dev.martianzoo.repl.JlineReplSmokeTest") }
    systemProperty("repl.shadowJar", named<ShadowJar>("shadowJar").get().archiveFile.get().asFile)
    systemProperty("repl.smokeScript", file("src/test/expect/repl-smoke.exp"))
  }
}
