pluginManagement { includeBuild("gradle/build-logic") }

// Enable Build Scans
// https://docs.gradle.org/current/userguide/github-actions.html#enable_build_scan_publishing
plugins { id("com.gradle.develocity") version ("4.2.2") }

// Temporary local checkout of the fork while validating its JS fix and WASM target.
includeBuild("_local/work4-better-parse")

develocity {
  buildScan {
    publishing.onlyIf { System.getenv("CI") != null }
    termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
    termsOfUseAgree.set("yes")
  }
}

rootProject.name = "solarnet"

val modules =
    listOf(
        "pets",
        "catalog",
        "tfm-text",
        "state",
        "engine",
        "agent",
        "tfm-state",
        "tfm-engine",
        "tfm-tests",
        "script",
        "repl",
        "tfm-card-data",
        "tfm-card-generator",
        "tfm-canon",
        "tfm-fake",
        "web",
        "almanac",
        "pets-tools",
        "viewer",
        "tools",
        "codegen",
        "benchmarks",
    )

include(*modules.toTypedArray())

modules.forEach { module -> project(":$module").projectDir = file("modules/$module") }

dependencyResolutionManagement {
  repositories { mavenCentral() }
}
