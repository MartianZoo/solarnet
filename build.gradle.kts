import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
  id("org.jetbrains.kotlin.jvm") version "2.1.20"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
  id("org.jetbrains.dokka") version "1.9.20"
}

repositories { mavenCentral() }

subprojects {
  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  apply(plugin = "org.jetbrains.dokka")

  tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
      displayName.set("Solarnet/Pets")
      documentedVisibilities.set(setOf(Visibility.PUBLIC, Visibility.PROTECTED))
      jdkVersion.set(21)
      skipEmptyPackages.set(true)
      suppressInheritedMembers.set(true)
    }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
      jvmToolchain(21)
    }
  }

  tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
      exceptionFormat = FULL
      showExceptions = true
      showStackTraces = true
    }
  }
}

tasks.dokkaHtmlMultiModule {
  moduleName.set("Solarnet/Pets")
  outputDirectory.set(rootProject.file("docs/api"))
  includes.from("docs/packages.md")
}

