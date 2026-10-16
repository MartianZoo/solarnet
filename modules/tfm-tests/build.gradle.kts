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
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.coroutines.test)
        implementation(project(":agent"))
        implementation(
            "dev.martianzoo.pets:catalog:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(project(":engine"))
        implementation("dev.martianzoo.pets:pets:${providers.gradleProperty("petsVersion").get()}")
        implementation(project(":script"))
        implementation(project(":state"))
        implementation(
            "dev.martianzoo.pets:tfm-canon:${providers.gradleProperty("petsVersion").get()}"
        )
        implementation(
            "dev.martianzoo.pets:tfm-fake:${providers.gradleProperty("petsVersion").get()}"
        )
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
val replayExportsEnabled =
    providers
        .gradleProperty("replayExports")
        .map { it.isEmpty() || it.toBooleanStrict() }
        .orElse(false)

val jvmTestCompilation = kotlin.targets.getByName("jvm").compilations.getByName("test")
val jvmTestRuntimeClasspath =
    configurations.named(requireNotNull(jvmTestCompilation.runtimeDependencyConfigurationName))

listOf("sandbox" to "TfmSandboxTest", "gameplay" to "TfmGameplayTest").forEach {
    (suite, fixtureName) ->
  tasks.register<Test>("${suite}Test") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs every $fixtureName subclass, including nested cases, without replay tests."
    // Shared tests cannot use JUnit's JVM-only @Tag, so select fixture subclasses from source.
    // Each file has one top-level class; nested fixture classes use its JVM binary-name prefix.
    val fixtureTestClasses =
        rootProject
            .fileTree("test") {
              include("common/dev/martianzoo/tfm/tests/**/*Test.kt")
              include("jvm/dev/martianzoo/tfm/tests/**/*Test.kt")
            }
            .files
            .flatMap { file ->
              val source = file.readText()
              val packageName =
                  requireNotNull(Regex("(?m)^package (\\S+)").find(source)?.groupValues?.get(1)) {
                    "Missing package declaration in $file"
                  }
              val topLevelClass =
                  requireNotNull(
                      Regex("(?m)^internal (?:abstract )?class (\\w+)")
                          .find(source)
                          ?.groupValues
                          ?.get(1)
                  ) {
                    "Missing top-level internal class in $file"
                  }
              val baseName = "$packageName.$topLevelClass"
              val fixture = "(?:dev\\.martianzoo\\.tfm\\.tests\\.)?$fixtureName"
              val direct = Regex("(?m)^internal class \\w+ : $fixture\\(").containsMatchIn(source)
              val nested =
                  Regex("(?m)^[ \\t]+internal class (\\w+) : $fixture\\(")
                      .findAll(source)
                      .map { "$baseName\$${it.groupValues[1]}" }
                      .toList()
              (if (direct) listOf(baseName) else emptyList()) + nested
            }
            .sorted()
    check(fixtureTestClasses.isNotEmpty()) { "No $fixtureName subclasses found" }

    outputs.cacheIf("rerun fixture tests for timing") { false }
    dependsOn(jvmTestCompilation.compileTaskProvider)
    testClassesDirs = jvmTestCompilation.output.classesDirs
    classpath =
        files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    filter { fixtureTestClasses.forEach { includeTestsMatching(it) } }
  }
}

tasks.withType<Test>().configureEach {
  extensions.configure<JacocoTaskExtension> { isEnabled = false }
}

// Curated imports from 2026, plus Game20230521Test retained for its explicit task replay.
// All other replay sources still compile. Review new games before adding them here.
val activeReplayClasses =
    listOf(
            "ActiveProtonFlowTest",
            "ActiveVacuumCoreTest",
            "DistantPressureMassTest",
            "DistantSignalBeamTest",
            "ErraticCarbonCurrentTest",
            "Game20230521Test",
            "MassiveGammaZoneTest",
            "OtbGame20260809Test",
            "OtbGame20260818Test",
            "OtbGame20260825Test",
            "OtbGame20260828Test",
            "OtbGame20260904Test",
            "OtbGame20260912Test",
            "OtbGame20261009Test",
            "SolarFusionStreamTest",
            "StaticPressureStreamTest",
            "StinaGameTest",
            "SyntheticMagnetBurstTest",
            "SyntheticPlasmaCurrentTest",
            "SyntheticProtonFragmentTest",
            "ThermalMatterWaveTest",
            "ThermalPlasmaStateTest",
            "Wsbg2025Test",
        )
        .map { "dev.martianzoo.tfm.tests.replays.$it" }

val replayTest by
    tasks.registering(Test::class) {
      description = "Runs the curated JVM replay suite."
    }
val replayCoverageTest by
    tasks.registering(Test::class) {
      description = "Runs the curated JVM replay suite with coverage instrumentation."
      extensions.configure<JacocoTaskExtension> { isEnabled = true }
      // Export runs share the same selected game files; serialize explicit requests for both tasks.
      mustRunAfter(replayTest)
    }

listOf(replayTest, replayCoverageTest).forEach { replayTask ->
  replayTask.configure {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    dependsOn(jvmTestCompilation.compileTaskProvider)
    testClassesDirs = jvmTestCompilation.output.classesDirs
    classpath =
        files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
    filter { activeReplayClasses.forEach { includeTestsMatching(it) } }
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
    systemProperty("solarnet.replayExports", replayExportsEnabled.get())
    systemProperty(
        "solarnet.replayEventLogDirectory",
        replayEventLogsDirectory.get().asFile.absolutePath,
    )
    if (replayExportsEnabled.get()) {
      outputs.dir(replayEventLogsDirectory)
      val activeNames = activeReplayClasses.map { it.substringAfterLast('.') }.toSet()
      val exportDirectory = replayEventLogsDirectory.get().asFile
      doFirst {
        exportDirectory.listFiles()?.forEach { file ->
          if (file.extension == "json" && file.name.substringBefore('.') !in activeNames) {
            file.delete()
          }
        }
      }
    }
  }
}

// Focused JVM requests remain independent of replays; aggregate verification includes both suites.
tasks.named<Test>("jvmTest") {
  filter { excludeTestsMatching("dev.martianzoo.tfm.tests.replays.*") }
}

listOf("test", "allTests").forEach { taskName ->
  tasks.named(taskName) { dependsOn(replayTest) }
}

// Intentionally unavailable. Uncomment only to run archived games; they always compile.
// tasks.register<Test>("archivedReplayTest") {
//   group = LifecycleBasePlugin.VERIFICATION_GROUP
//   description = "Runs the archived JVM replays."
//   dependsOn(jvmTestCompilation.compileTaskProvider)
//   testClassesDirs = jvmTestCompilation.output.classesDirs
//   classpath =
//       files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
//   filter {
//     includeTestsMatching("dev.martianzoo.tfm.tests.replays.*")
//     activeReplayClasses.forEach { excludeTestsMatching(it) }
//   }
// }

val runtimeProjectArtifacts = jvmTestRuntimeClasspath.map { runtimeClasspath ->
  runtimeClasspath.incoming
      .artifactView { componentFilter { it is ProjectComponentIdentifier } }
      .files
}

tasks.register<JacocoReport>("replayTestCoverage") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs only the JVM replay tests and reports their production-code coverage."
  dependsOn(replayCoverageTest)
  executionData(
      replayCoverageTest.map { task ->
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
