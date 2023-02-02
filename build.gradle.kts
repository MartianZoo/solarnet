import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.0"
  id("org.jetbrains.dokka") version "1.7.10"
}

repositories { mavenCentral() }

subprojects {
  repositories { mavenCentral() }

  apply(plugin = "org.jetbrains.dokka")

  tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
      displayName.set("SolarNet")
      documentedVisibilities.set(setOf(Visibility.PUBLIC, Visibility.PROTECTED))
      jdkVersion.set(17)
      skipEmptyPackages.set(true)
      suppressInheritedMembers.set(true)
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

dependencies {
  implementation(kotlin("stdlib"))
}
