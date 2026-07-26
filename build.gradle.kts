import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.kotlin.KtfmtStep.TrailingCommaManagementStrategy.ONLY_ADD
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id("dev.detekt") version "2.0.0-alpha.5" apply false
  id("com.diffplug.spotless") version "8.8.0"
  id("org.jetbrains.kotlin.jvm") version "2.4.10"
  id("org.jetbrains.kotlin.multiplatform") version "2.4.10" apply false
  id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
  id("org.jetbrains.dokka") version "2.2.0"
}

repositories { mavenCentral() }

val pinnedYarnResolutions =
    mapOf(
        "serialize-javascript" to "7.0.3",
        "fast-uri" to "3.1.4",
    )

rootProject.plugins.withType<YarnPlugin> {
  val yarn = rootProject.the<YarnRootExtension>()
  rootProject.the<YarnRootEnvSpec>().version.set("1.22.22")
  pinnedYarnResolutions.forEach(yarn::resolution)

  // Kotlin 2.2.21 does not track Yarn resolutions as inputs to this generated file. Without this,
  // a stale build/js/package.json can omit new resolutions and repeatedly fight the lockfile.
  rootProject.tasks.named("rootPackageJson") {
    inputs.property("pinnedYarnResolutions", pinnedYarnResolutions)
  }
}

configure<SpotlessExtension> {
  kotlin {
    target(subprojects.map { it.fileTree("src") { include("**/*.kt") } })
    ktfmt("0.64").googleStyle().configure {
      it.setMaxWidth(100)
      it.setBlockIndent(2)
      it.setContinuationIndent(4)
      it.setTrailingCommaManagementStrategy(ONLY_ADD)
    }
  }
  kotlinGradle {
    target(
        files(
            "build.gradle.kts",
            "settings.gradle.kts",
            subprojects.map { it.file("build.gradle.kts") },
        )
    )
    ktfmt("0.64").googleStyle().configure {
      it.setMaxWidth(100)
      it.setBlockIndent(2)
      it.setContinuationIndent(4)
      it.setTrailingCommaManagementStrategy(ONLY_ADD)
    }
  }
}

subprojects {
  apply(plugin = "dev.detekt")

  extensions.configure<DetektExtension> {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("detekt.yml"))
  }

  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
      allWarningsAsErrors.set(true)
      languageVersion.set(KotlinVersion.KOTLIN_2_2)
      apiVersion.set(KotlinVersion.KOTLIN_2_2)
      freeCompilerArgs.addAll(
          "-Wextra",
          "-Xwarning-level=REDUNDANT_VISIBILITY_MODIFIER:disabled",
          "-Xwarning-level=RETURN_VALUE_NOT_USED:disabled",
      )
    }
  }

  tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
      freeCompilerArgs.add("-Xjdk-release=17")
    }
  }

  tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
  }

  configurations
      .matching { it.name != "detekt" }
      .configureEach {
        resolutionStrategy.eachDependency {
          if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.4.10")
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
      jdkVersion.set(17)
      skipEmptyPackages.set(true)
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
          ":script:copyCanonResourcesForKarma",
          ":script:copyPetsResourcesForKarma",
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
  dokka(project(":web"))
}

tasks.register<Exec>("installGitHooks") {
  group = "build setup"
  description = "Configures Git to use the repository's versioned hooks."
  commandLine("git", "config", "core.hooksPath", "githooks")
}
