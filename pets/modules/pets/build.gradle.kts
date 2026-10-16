import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  id("pets.kmp-jvm-js")
  jacoco
}

val commonSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/pets")
val commonTestDirectory = rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/pets")

kotlin {
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser()
    nodejs()
  }

  sourceSets {
    commonMain {
      kotlin.setSrcDirs(listOf(commonSourceDirectory))
      dependencies {
        implementation(libs.better.parse)
      }
    }
    commonTest {
      kotlin.setSrcDirs(
          listOf(
              commonTestDirectory,
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/testsupport"),
          )
      )
      dependencies { implementation(libs.kotest.assertions.core) }
    }
    jsMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/js/dev/martianzoo/pets"))
      )
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/pets"))
      )
    }
    jvmMain {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/pets"))
      )
    }
    jvmTest {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/pets"))
      )
    }
  }
}

val allBrowserTestsRequested = rootProject.extra["allBrowserTestsRequested"] as Boolean

tasks.named<org.gradle.api.tasks.testing.AbstractTestTask>("jsBrowserTest") {
  if (!allBrowserTestsRequested) {
    filter.includeTestsMatching("dev.martianzoo.pets.BrowserPetsTest")
  }
}

tasks.named("test") {
  description = "Runs Pets JVM tests and browser-specific tests."
  dependsOn("jsBrowserTest")
}

val jvmMainCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
val jvmTest by tasks.existing(Test::class)

tasks.register<JacocoReport>("jvmTestCoverage") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs the Pets JVM test suite and reports Pets production-code coverage."
  dependsOn(jvmTest)
  executionData(
      jvmTest.map { task ->
        requireNotNull(task.extensions.getByType<JacocoTaskExtension>().destinationFile)
      }
  )
  classDirectories.from(jvmMainCompilation.output.classesDirs)
  sourceDirectories.from(
      commonSourceDirectory,
      rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/pets"),
  )
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}
