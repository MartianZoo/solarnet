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
            "BinaryVectorStreamTest",
            "CosmicProtonTraceTest",
            "DistantPressureMassTest",
            "DistantSignalBeamTest",
            "ErraticCarbonCurrentTest",
            "Game20230521Test",
            "KineticCarbonStateTest",
            "MassiveGammaZoneTest",
            "OtbGame20260809Test",
            "OtbGame20260818Test",
            "OtbGame20260825Test",
            "OtbGame20260828Test",
            "OtbGame20260904Test",
            "OtbGame20260912Test",
            "OtbGame20261009Test",
            "OtbGame20261111Test",
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

val checkReplayLiterals by tasks.registering {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Rejects calculated quantities in ordinary replay instruction strings."
  val sources =
      rootProject.fileTree("test") {
        include("common/dev/martianzoo/tfm/tests/replays/*Test.kt")
        include("jvm/dev/martianzoo/tfm/tests/replays/*Test.kt")
        exclude("**/OtbGame20260828Test.kt")
      }
  inputs.files(sources)
  doLast {
    // Skip comments and raw strings containing configuration or embedded Pets declarations.
    val tokens =
        Regex(
            "/\\*.*?\\*/|//[^\\n]*|\"\"\".*?\"\"\"|\"((?:\\\\.|[^\"\\\\])*)\"",
            RegexOption.DOT_MATCHES_ALL,
        )
    sources.files.forEach { file ->
      val source = file.readText()
      tokens.findAll(source).forEach { token ->
        val literal = token.groups[1]?.value ?: return@forEach
        check('/' !in literal) {
          val line = source.take(token.range.first).count { it == '\n' } + 1
          "${file.name}:$line: replay instructions must use literal quantities: $literal"
        }
      }
    }
  }
}

tasks.named("check") { dependsOn(checkReplayLiterals) }

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
    dependsOn(jvmTestCompilation.compileTaskProvider, checkReplayLiterals)
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
      val exportDirectory = replayEventLogsDirectory.get().asFile
      doFirst {
        exportDirectory.listFiles()?.forEach { file ->
          if (file.extension == "json") {
            file.delete()
          }
        }
      }
    }
  }
}

// Focused JVM requests remain independent of replays; aggregate verification includes both suites.
tasks.named<Test>("jvmTest") {
  filter {
    excludeTestsMatching("dev.martianzoo.tfm.tests.replays.*")
    // The evolving human archive is an opt-in local input.
    excludeTestsMatching("dev.martianzoo.tfm.tests.archive.ArchiveDraftTest")
  }
}

listOf("test", "allTests").forEach { taskName ->
  tasks.named(taskName) { dependsOn(replayTest) }
}

// Older scenarios remain opt-in; each also verifies its checked-in archive.
tasks.register<Test>("archivedReplayTest") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs the archived JVM replays and verifies their checked-in game files."
  dependsOn(jvmTestCompilation.compileTaskProvider, checkReplayLiterals)
  testClassesDirs = jvmTestCompilation.output.classesDirs
  classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
  filter {
    includeTestsMatching("dev.martianzoo.tfm.tests.replays.*")
    activeReplayClasses.forEach { excludeTestsMatching(it) }
  }
  systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
  systemProperty("solarnet.replayExports", replayExportsEnabled.get())
  systemProperty(
      "solarnet.replayEventLogDirectory",
      replayEventLogsDirectory.get().asFile.absolutePath,
  )
  if (replayExportsEnabled.get()) outputs.dir(replayEventLogsDirectory)
  mustRunAfter(replayTest, replayCoverageTest)
}

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

// Opt-in prototype: the human-edited archive stays under _local until its format settles.
tasks.register<Test>("archiveDraftTest") {
  description = "Checks the local human-readable archive against the Game20261111 replay."
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  dependsOn(jvmTestCompilation.compileTaskProvider)
  testClassesDirs = jvmTestCompilation.output.classesDirs
  classpath = files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles)
  filter { includeTestsMatching("dev.martianzoo.tfm.tests.archive.ArchiveDraftTest") }
  val archive =
      providers
          .gradleProperty("archiveFile")
          .orElse(rootProject.file("_local/notes/game20261111-format-draft.game").absolutePath)
  inputs.file(archive)
  systemProperty("archive.file", archive.get())
  val archiveOutput =
      providers
          .gradleProperty("archiveOutput")
          .orElse(
              layout.buildDirectory.file("generated/archive/game20261111.game").map {
                it.asFile.absolutePath
              }
          )
  outputs.file(archiveOutput)
  systemProperty("archive.output", archiveOutput.get())
}
