import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
  id("solarnet.kmp-jvm-js")
  jacoco
}

kotlin {
  sourceSets {
    commonTest {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir(
                  "test/common/dev/martianzoo/agenttestsupport"
              ),
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/testsupport"),
              rootProject.layout.projectDirectory.dir("test/common/dev/martianzoo/tfm/tests"),
          )
      )
      kotlin.exclude("**/replays/**")
      kotlin.exclude("**/curiosities/**")
      dependencies {
        implementation(libs.kotest.assertions.core)
        implementation(project(":agent"))
        implementation(project(":catalog"))
        implementation(project(":engine"))
        implementation(project(":pets"))
        implementation(project(":script"))
        implementation(project(":state"))
        implementation(project(":tfm-canon"))
        implementation(project(":tfm-fake"))
        implementation(project(":tfm-engine"))
        implementation(project(":tfm-state"))
      }
    }
    jsTest {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir(
                  "test/common/dev/martianzoo/tfm/tests/replays"
              ),
              rootProject.layout.projectDirectory.dir(
                  "test/common/dev/martianzoo/tfm/tests/curiosities"
              ),
              rootProject.layout.projectDirectory.dir("test/js/dev/martianzoo/tfm/tests"),
              rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tfm/tests/replays"),
          )
      )
      // The replay scenarios are portable; their JUnit file-export hook is JVM-only.
      kotlin.exclude("ReplayExportExtension.kt")
    }
    jvmTest {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir(
                  "test/common/dev/martianzoo/tfm/tests/replays"
              ),
              rootProject.layout.projectDirectory.dir(
                  "test/common/dev/martianzoo/tfm/tests/curiosities"
              ),
              rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tfm/tests"),
          )
      )
    }
  }
}

val replayEventLogsDirectory = layout.buildDirectory.dir("generated/replay-event-logs")

tasks.named<Test>("jvmTest") {
  systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
  systemProperty(
      "solarnet.replayEventLogDirectory",
      replayEventLogsDirectory.get().asFile.absolutePath,
  )
  outputs.dir(replayEventLogsDirectory)
}

val jvmTestCompilation = kotlin.targets.getByName("jvm").compilations.getByName("test")
val jvmTestRuntimeClasspath =
    configurations.named(requireNotNull(jvmTestCompilation.runtimeDependencyConfigurationName))

tasks.withType<Test>().configureEach {
  extensions.configure<JacocoTaskExtension> { isEnabled = false }
}

val replayTest by
    tasks.registering(Test::class) {
      group = LifecycleBasePlugin.VERIFICATION_GROUP
      description = "Runs only the JVM replay-test suite."
      dependsOn(jvmTestCompilation.compileTaskProvider)
      testClassesDirs = jvmTestCompilation.output.classesDirs
      classpath =
          files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
      filter { includeTestsMatching("dev.martianzoo.tfm.tests.replays.*") }
      extensions.configure<JacocoTaskExtension> { isEnabled = true }
    }

val runtimeProjectArtifacts = jvmTestRuntimeClasspath.map { runtimeClasspath ->
  runtimeClasspath.incoming
      .artifactView { componentFilter { it is ProjectComponentIdentifier } }
      .files
}

tasks.register<JacocoReport>("replayTestCoverage") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs only the JVM replay tests and reports their production-code coverage."
  dependsOn(replayTest)
  executionData(
      replayTest.map { task ->
        requireNotNull(task.extensions.getByType<JacocoTaskExtension>().destinationFile)
      }
  )
  classDirectories.from(runtimeProjectArtifacts)
  sourceDirectories.from(
      rootProject.layout.projectDirectory.dir("src/common"),
      rootProject.layout.projectDirectory.dir("src/jvm"),
  )
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}
