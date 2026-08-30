import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("solarnet.jvm")
  alias(libs.plugins.shadow)
  `java-library`
}

kotlin {
  sourceSets {
    main {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/repl"))
      )
    }
    test {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/repl"))
      )
    }
  }
}

dependencies {
  implementation(project(":script"))
  implementation(libs.jline)

  testImplementation(libs.truth)
}

val shadowJar = tasks.named<ShadowJar>("shadowJar")

shadowJar.configure {
  mergeServiceFiles()
  manifest { attributes(mapOf("Main-Class" to "dev.martianzoo.repl.JlineReplKt")) }
}

// The smoke test needs a real terminal, so it is excluded from `test` and gets its own task.
tasks.test { filter { excludeTestsMatching("dev.martianzoo.repl.JlineReplSmokeTest") } }

tasks.register<Test>("realTerminalSmokeTest") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs the REPL smoke test in a real terminal session using Expect."
  dependsOn(shadowJar)
  testClassesDirs = sourceSets.test.get().output.classesDirs
  classpath = sourceSets.test.get().runtimeClasspath
  filter { includeTestsMatching("dev.martianzoo.repl.JlineReplSmokeTest") }
  systemProperty("repl.shadowJar", shadowJar.get().archiveFile.get().asFile)
  systemProperty(
      "repl.smokeScript",
      rootProject.layout.projectDirectory.file("test/jvm/dev/martianzoo/repl/repl-smoke.exp"),
  )
}
