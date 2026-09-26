import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
  alias(libs.plugins.spotless)
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.dokka)
}

val allBrowserTestsRequested =
    gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "allBrowserTests" }

extra["allBrowserTestsRequested"] = allBrowserTestsRequested

// Kotlin creates a browser-test task for every JS target. Only :web:jsBrowserTest is part of the
// normal test suite; the rest are inert unless the deliberately unavailable full-browser target
// below is temporarily restored.
subprojects {
  if (name != "web") {
    tasks
        .matching { it.name == "jsBrowserTest" }
        .configureEach {
          description = "Disabled except through the temporary full-browser test target."
          inputs.property("allBrowserTestsRequested", allBrowserTestsRequested)
          onlyIf("only the repository browser suite runs routinely") { task ->
            task.inputs.properties["allBrowserTestsRequested"] == true
          }
        }
  }
}

// This is intentionally not an available Gradle target. Temporarily uncomment it only when the
// low-value, very slow exercise of every browser-compatible test is specifically wanted.
// tasks.register("allBrowserTests") {
//   group = LifecycleBasePlugin.VERIFICATION_GROUP
//   description = "Runs every browser-compatible test in a browser."
//   dependsOn(subprojects.map { it.tasks.matching { task -> task.name == "jsBrowserTest" } })
// }

val pinnedYarnResolutions =
    mapOf(
        "body-parser" to "1.20.6",
        "brace-expansion" to "2.1.4",
        "browserslist" to "4.28.8",
        "diff" to "8.0.3",
        "fast-uri" to "3.1.6",
        "js-yaml" to "4.3.1",
        "nanoid" to "3.3.18",
        "qs" to "6.16.0",
        "serialize-javascript" to "7.0.5",
        "socket.io-parser" to "4.2.7",
        "uuid" to "11.1.1",
        "webpack" to "5.104.1",
        "webpack-dev-server" to "5.2.6",
    )

plugins.withType<YarnPlugin> {
  the<YarnRootEnvSpec>().version.set("1.22.22")
  pinnedYarnResolutions.forEach(the<YarnRootExtension>()::resolution)

  // Kotlin does not track Yarn resolutions as inputs to this generated file. Without this,
  // a stale build/js/package.json can omit new resolutions and repeatedly fight the lockfile.
  tasks.named("rootPackageJson") { inputs.property("pinnedYarnResolutions", pinnedYarnResolutions) }
}

// ktfmt's default (Meta) style is exactly this project's style: 100 columns, 2-space block indent,
// 4-space continuation indent, and trailing commas added but never removed.
spotless {
  kotlin {
    target("src/**/*.kt", "test/**/*.kt")
    ktfmt(libs.versions.ktfmt.get())
  }
  kotlinGradle {
    target(
        "*.gradle.kts",
        "modules/*/*.gradle.kts",
        "gradle/build-logic/src/main/kotlin/*.gradle.kts",
    )
    ktfmt(libs.versions.ktfmt.get())
  }
}

dokka {
  moduleName.set("Solarnet")
  dokkaPublications.html {
    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    includes.from("docs/packages.md")
  }
}

dependencies {
  dokka(project(":pets"))
  dokka(project(":state"))
  dokka(project(":engine"))
  dokka(project(":tfm-engine"))
  dokka(project(":script"))
  dokka(project(":repl"))
  dokka(project(":tfm-canon"))
  dokka(project(":tfm-fake"))
  dokka(project(":web"))
  dokka(project(":almanac"))
  dokka(project(":game-viewer"))
}

tasks.register<Exec>("installGitHooks") {
  group = "build setup"
  description = "Configures Git to use the repository's versioned hooks."
  commandLine("git", "config", "core.hooksPath", "githooks")
}

tasks.register("webAppsDevelopmentRun") {
  group = "run"
  description = "Starts one development server for every browser app."
  dependsOn(":game-viewer:jsBrowserDevelopmentRun")
}
