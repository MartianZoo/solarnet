pluginManagement { includeBuild("gradle/build-logic") }

// Enable Build Scans
// https://docs.gradle.org/current/userguide/github-actions.html#enable_build_scan_publishing
plugins {
  id("com.gradle.develocity") version ("4.2.2")
}

develocity {
  buildScan {
    publishing.onlyIf { System.getenv("CI") != null }
    termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
    termsOfUseAgree.set("yes")
  }
}

rootProject.name = "solarnet"

include("pets", "language", "engine", "script", "repl", "tfm-canon", "web", "tools", "benchmarks")

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    exclusiveContent {
      forRepository { maven { url = uri("https://jitpack.io") } }
      filter { includeGroup("com.github.kevinb9n.better-parse") }
    }
  }
}
