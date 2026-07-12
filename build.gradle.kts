import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.kotlin.KtfmtStep.TrailingCommaManagementStrategy.ONLY_ADD
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
  id("com.diffplug.spotless") version "8.8.0"
  id("org.jetbrains.kotlin.jvm") version "2.2.21"
  id("org.jetbrains.kotlin.multiplatform") version "2.2.21" apply false
  id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
  id("org.jetbrains.dokka") version "2.2.0"
}

repositories { mavenCentral() }

configure<SpotlessExtension> {
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**")
    ktfmt("0.64").googleStyle().configure {
      it.setMaxWidth(100)
      it.setBlockIndent(2)
      it.setContinuationIndent(4)
      it.setTrailingCommaManagementStrategy(ONLY_ADD)
    }
  }
  kotlinGradle {
    target("**/*.gradle.kts")
    ktfmt("0.64").googleStyle().configure {
      it.setMaxWidth(100)
      it.setBlockIndent(2)
      it.setContinuationIndent(4)
      it.setTrailingCommaManagementStrategy(ONLY_ADD)
    }
  }
}

subprojects {
  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  configurations.configureEach {
    resolutionStrategy.eachDependency {
      if (requested.group == "org.jetbrains.kotlin") {
        useVersion("2.2.21")
        because("Kotlin/JS compilation requires libraries compiled for the project Kotlin version")
      }
    }
  }

  apply(plugin = "org.jetbrains.dokka")

  extensions.configure<DokkaExtension> {
    dokkaPublications.configureEach {
      suppressInheritedMembers.set(true)
    }
    dokkaSourceSets.configureEach {
      documentedVisibilities.set(setOf(VisibilityModifier.Public, VisibilityModifier.Protected))
      jdkVersion.set(21)
      skipEmptyPackages.set(true)
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

tasks
    .matching { it.name == "rootPackageJson" }
    .configureEach {
      dependsOn(
          ":canon:copyCanonResourcesForKarma",
          ":engine:copyCanonResourcesForKarma",
          ":pets:copyCanonResourcesForKarma",
      )
    }

dokka {
  moduleName.set("Solarnet/Pets")
  dokkaPublications.html {
    outputDirectory.set(rootProject.file("docs/api"))
    includes.from("docs/packages.md")
  }
}

dependencies {
  dokka(project(":pets"))
  dokka(project(":engine"))
  dokka(project(":script"))
  dokka(project(":repl"))
  dokka(project(":canon"))
}
