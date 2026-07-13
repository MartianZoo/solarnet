import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.kotlin.KtfmtStep.TrailingCommaManagementStrategy.ONLY_ADD
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
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
  apply(plugin = "io.gitlab.arturbosch.detekt")

  extensions.configure<DetektExtension> {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("detekt.yml"))
  }

  tasks.matching { it.name == "check" }.configureEach {
    dependsOn(
        tasks.matching {
          it.name in setOf("detektMain", "detektTest", "detektJvmMain", "detektJvmTest")
        }
    )
  }

  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.allWarningsAsErrors.set(true)
    compilerOptions.freeCompilerArgs.addAll(
        "-Wextra",
        "-Xwarning-level=REDUNDANT_VISIBILITY_MODIFIER:disabled",
        "-Xwarning-level=RETURN_VALUE_NOT_USED:disabled",
    )
  }

  configurations
      .matching { it.name != "detekt" }
      .configureEach {
        resolutionStrategy.eachDependency {
          if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.2.21")
            because(
                "Kotlin/JS compilation requires libraries compiled for the project Kotlin version"
            )
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
  moduleName.set("Solarnet")
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

tasks.register<Exec>("installGitHooks") {
  group = "build setup"
  description = "Configures Git to use the repository's versioned hooks."
  commandLine("git", "config", "core.hooksPath", "githooks")
}
