import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.Test

plugins {
  id("solarnet.jvm")
  alias(libs.plugins.shadow)
  `java-library`
}

dependencies {
  implementation(project(":script"))
  implementation(libs.jline)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.truth)
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
