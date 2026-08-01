import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.kotlin.KtfmtStep.TrailingCommaManagementStrategy.ONLY_ADD
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
  alias(libs.plugins.spotless)
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.dokka)
}

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
    target("*/src/**/*.kt")
    ktfmt("0.64").googleStyle().configure {
      it.setMaxWidth(100)
      it.setBlockIndent(2)
      it.setContinuationIndent(4)
      it.setTrailingCommaManagementStrategy(ONLY_ADD)
    }
  }
  kotlinGradle {
    target(
        "*.gradle.kts",
        "*/build.gradle.kts",
        "build-logic/src/main/kotlin/*.gradle.kts",
    )
    ktfmt("0.64").googleStyle().configure {
      it.setMaxWidth(100)
      it.setBlockIndent(2)
      it.setContinuationIndent(4)
      it.setTrailingCommaManagementStrategy(ONLY_ADD)
    }
  }
}

tasks.register("test") {
  description = "Runs all repository JVM test suites."
  dependsOn(
      ":canon:jvmTest",
      ":engine:jvmTest",
      ":pets:jvmTest",
      ":script:jvmTest",
      ":repl:test",
      ":tools:test",
  )
}

tasks.named("check") { dependsOn("test") }

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
