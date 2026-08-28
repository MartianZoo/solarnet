import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
  alias(libs.plugins.spotless)
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.dokka)
}

val fullBrowserTestsRequested =
    providers.gradleProperty("includeBrowserTests").orNull?.toBoolean() == true ||
        gradle.startParameter.taskNames.any {
          it.substringAfterLast(':') in setOf("jsBrowserTest", "allTestsIncludingBrowser")
        }

// JVM tests provide the exhaustive routine signal. Browser tests are opt-in except for the one
// representative Terraforming Mars smoke scenario configured in tfm-tests/build.gradle.kts.
subprojects {
  if (name != "tfm-tests") {
    tasks
        .matching { it.name == "jsBrowserTest" }
        .configureEach {
          inputs.property("fullBrowserTestsRequested", fullBrowserTestsRequested)
          onlyIf("full browser tests were explicitly requested") { task ->
            task.inputs.properties["fullBrowserTestsRequested"] == true
          }
        }
  }
}

val pinnedYarnResolutions = mapOf("serialize-javascript" to "7.0.3", "fast-uri" to "3.1.4")

plugins.withType<YarnPlugin> {
  the<YarnRootEnvSpec>().version.set("1.22.22")
  pinnedYarnResolutions.forEach(the<YarnRootExtension>()::resolution)

  // Kotlin 2.2.21 does not track Yarn resolutions as inputs to this generated file. Without this,
  // a stale build/js/package.json can omit new resolutions and repeatedly fight the lockfile.
  tasks.named("rootPackageJson") { inputs.property("pinnedYarnResolutions", pinnedYarnResolutions) }
}

// ktfmt's default (Meta) style is exactly this project's style: 100 columns, 2-space block indent,
// 4-space continuation indent, and trailing commas added but never removed.
spotless {
  kotlin {
    target("*/src/**/*.kt")
    ktfmt(libs.versions.ktfmt.get())
  }
  kotlinGradle {
    target("*.gradle.kts", "*/*.gradle.kts", "gradle/build-logic/src/main/kotlin/*.gradle.kts")
    ktfmt(libs.versions.ktfmt.get())
  }
}

dokka {
  moduleName.set("Solarnet")
  dokkaPublications.html {
    outputDirectory.set(file("docs/api"))
    includes.from("docs/packages.md")
  }
}

dependencies {
  dokka(project(":pets"))
  dokka(project(":tfm-text"))
  dokka(project(":engine"))
  dokka(project(":tfm-engine"))
  dokka(project(":script"))
  dokka(project(":repl"))
  dokka(project(":tfm-canon"))
  dokka(project(":web"))
}

tasks.register<Exec>("installGitHooks") {
  group = "build setup"
  description = "Configures Git to use the repository's versioned hooks."
  commandLine("git", "config", "core.hooksPath", "githooks")
}
