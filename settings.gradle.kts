pluginManagement { includeBuild("gradle/build-logic") }

// Enable Build Scans
// https://docs.gradle.org/current/userguide/github-actions.html#enable_build_scan_publishing
plugins { id("com.gradle.develocity") version ("4.2.2") }

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
        "state",
        "engine",
        "agent",
        "tfm-state",
        "tfm-engine",
        "tfm-tests",
        "script",
        "repl",
        "web",
        "tools",
        "benchmarks",
    )

include(*modules.toTypedArray())

modules.forEach { module -> project(":$module").projectDir = file("modules/$module") }

// _local may be shared by several worktrees; each working copy has its own selection file.
val petsSelectionId = "${rootDir.name}-${rootDir.canonicalPath.hashCode().toUInt().toString(16)}"
val petsSelectionFile = file("_local/pets-sources/$petsSelectionId.properties")
val petsSelection =
    java.util.Properties().apply {
      if (petsSelectionFile.isFile) petsSelectionFile.inputStream().use { load(it) }
    }
val petsSource =
    providers
        .gradleProperty("petsSource")
        .orElse(petsSelection.getProperty("petsSource") ?: "pets")
        .get()
val petsVersion = providers.gradleProperty("petsVersion").get()

val petsDescription =
    if (petsSource == "artifacts") {
      "Pets: artifacts $petsVersion"
    } else {
      val source = file(petsSource).canonicalFile
      require(source.resolve("settings.gradle.kts").isFile) {
        "Pets source does not contain settings.gradle.kts: $source. " +
            "Set -PpetsSource=/path/to/pets or -PpetsSource=artifacts; local selection: $petsSelectionFile"
      }
      includeBuild(source) { name = "pets" }
      "Pets: local source $source (declared release $petsVersion)"
    }

gradle.extra["petsDescription"] = "$petsDescription\nPets selection file: $petsSelectionFile"

dependencyResolutionManagement {
  repositories {
    providers.gradleProperty("petsRepository").orNull?.let { repository ->
      exclusiveContent {
        forRepository { maven { url = uri(repository) } }
        filter { includeGroup("dev.martianzoo.pets") }
      }
    }
    mavenCentral()
    exclusiveContent {
      forRepository { maven { url = uri("https://jitpack.io") } }
      filter { includeGroup("com.github.kevinb9n.better-parse") }
    }
  }
}
