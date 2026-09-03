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

// Build output is relocated per user by gradle/user-isolation.init.gradle.kts, so the launcher
// scripts ask Gradle where the jar landed instead of assuming a path.
tasks.register("shadowJarPath") {
  group = LifecycleBasePlugin.BUILD_GROUP
  description = "Builds the REPL fat jar and prints its absolute path."
  val jarFile = shadowJar.flatMap(ShadowJar::getArchiveFile)
  doLast { println(jarFile.get().asFile.absolutePath) }
}
