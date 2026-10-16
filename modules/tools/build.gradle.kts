import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.PathSensitivity

plugins {
  id("solarnet.jvm")
}

val kotlinFileComplexityAnalyzer by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = true
}

val toolsSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tfm/tools")

kotlin {
  sourceSets {
    main { kotlin.setSrcDirs(listOf(toolsSourceDirectory)) }
    test {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tfm/tools"))
      )
    }
  }
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(project(":agent"))
  implementation("dev.martianzoo.pets:catalog:${providers.gradleProperty("petsVersion").get()}")
  implementation("dev.martianzoo.pets:tfm-canon:${providers.gradleProperty("petsVersion").get()}")
  implementation("dev.martianzoo.pets:tfm-fake:${providers.gradleProperty("petsVersion").get()}")
  implementation(project(":engine"))
  implementation("dev.martianzoo.pets:pets:${providers.gradleProperty("petsVersion").get()}")
  implementation(project(":state"))
  implementation(project(":tfm-engine"))
  kotlinFileComplexityAnalyzer(libs.detekt.metrics)
  testRuntimeOnly(libs.detekt.metrics)
}

tasks.withType<Test>().configureEach {
  systemProperty("solarnet.root", rootProject.layout.projectDirectory.asFile.absolutePath)
}

tasks.register<JavaExec>("typeStructureReport") {
  group = "application"
  description = "Reports encoding-relevant type statistics for an all-expansions five-player game."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.tools.TypeStructureReportKt")
}

val eventLogDumpOutput =
    rootProject.layout.projectDirectory.file(
        "_local/eventlogs/three-player-all-expansions-eventlog.tsv"
    )
val soloEventLogDumpOutput =
    rootProject.layout.projectDirectory.file("_local/eventlogs/solo-all-expansions-eventlog.tsv")
val otbGame20260828EventLogDumpOutput =
    rootProject.layout.projectDirectory.file("_local/eventlogs/otb-game-20260828-eventlog.tsv")
val otbGame20260912EventLogDumpOutput =
    rootProject.layout.projectDirectory.file("_local/eventlogs/otb-game-20260912-eventlog.tsv")
val replayEventLogsDirectory =
    project(":tfm-tests").layout.buildDirectory.dir("generated/replay-event-logs")
val replayExportsEnabled =
    providers
        .gradleProperty("replayExports")
        .map { it.isEmpty() || it.toBooleanStrict() }
        .orElse(false)

tasks.register<JavaExec>("buildReplayIndex") {
  group = "build"
  description = "Extracts configurations and completed scores for the Playground game chooser."
  if (replayExportsEnabled.get()) dependsOn(":tfm-tests:replayTest")
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.tools.BuildReplayIndexKt")
  val output = layout.buildDirectory.file("generated/replay-index/index.json")
  inputs.dir(replayEventLogsDirectory)
  outputs.file(output)
  args(replayEventLogsDirectory.get().asFile.absolutePath, output.get().asFile.absolutePath)
}

tasks.register<JavaExec>("dumpAllExpansionsEventLogs") {
  group = "reporting"
  description = "Dumps three-player and solo all-expansions change-event logs as TSV."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.tools.DumpEventlogKt")
  args(eventLogDumpOutput.asFile.absolutePath, soloEventLogDumpOutput.asFile.absolutePath)
}

tasks.register<JavaExec>("dumpOtbGame20260828EventLog") {
  group = "reporting"
  description = "Dumps the generated 2026-08-28 replay-test event log as TSV."
  val exportsEnabled = replayExportsEnabled.get()
  if (exportsEnabled) dependsOn(":tfm-tests:replayTest")
  doFirst { check(exportsEnabled) { "Pass -PreplayExports to generate replay files." } }
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.tools.DumpEventlogKt")
  args(
      replayEventLogsDirectory
          .map { it.file("OtbGame20260828Test.json").asFile.absolutePath }
          .get(),
      otbGame20260828EventLogDumpOutput.asFile.absolutePath,
  )
}

tasks.register<JavaExec>("dumpOtbGame20260912EventLog") {
  group = "reporting"
  description = "Dumps the generated 2026-09-12 replay-test event log as TSV."
  val exportsEnabled = replayExportsEnabled.get()
  if (exportsEnabled) dependsOn(":tfm-tests:replayTest")
  doFirst { check(exportsEnabled) { "Pass -PreplayExports to generate replay files." } }
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.tools.DumpEventlogKt")
  args(
      replayEventLogsDirectory
          .map { it.file("OtbGame20260912Test.json").asFile.absolutePath }
          .get(),
      otbGame20260912EventLogDumpOutput.asFile.absolutePath,
  )
}

val kotlinFileComplexitySources =
    rootProject.layout.projectDirectory.asFileTree.matching {
      include("src/**/*.kt")
      include("test/**/*.kt")
      include("pets/src/**/*.kt")
      include("pets/test/**/*.kt")
      exclude("pets/src/**/dev/martianzoo/tfm/petstools/**")
      exclude("pets/src/**/dev/martianzoo/tfm/text/**")
      exclude("pets/test/**/dev/martianzoo/tfm/petstools/**")
      exclude("pets/test/**/dev/martianzoo/tfm/text/**")
      exclude("src/**/dev/martianzoo/tfm/petstools/**")
      exclude("src/**/dev/martianzoo/tfm/tools/**")
      exclude("test/**/dev/martianzoo/tfm/petstools/**")
      exclude("test/**/dev/martianzoo/tfm/tools/**")
      exclude("test/**/dev/martianzoo/tfm/benchmarks/**")
      exclude("src/**/dev/martianzoo/tfm/text/**")
      exclude("test/**/dev/martianzoo/tfm/text/**")
    }
val kotlinFileComplexityReport =
    rootProject.layout.buildDirectory.file("reports/kotlin-file-complexity.tsv")
val kotlinFileComplexityState =
    rootProject.layout.buildDirectory.file("kotlinFileComplexity/state.tsv")

tasks.register<JavaExec>("kotlinFileComplexity") {
  group = "reporting"
  description =
      "Writes cyclomatic complexity for production Kotlin and reusable test infrastructure."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.tools.KotlinFileComplexityKt")
  inputs.files(kotlinFileComplexitySources).withPathSensitivity(PathSensitivity.RELATIVE)
  inputs.files(kotlinFileComplexityAnalyzer).withNormalizer(ClasspathNormalizer::class.java)
  outputs.file(kotlinFileComplexityReport)
  outputs.file(kotlinFileComplexityState)
  systemProperty(
      "solarnet.kotlinFileComplexity.detektClasspath",
      kotlinFileComplexityAnalyzer.asPath,
  )
  args(
      rootProject.layout.projectDirectory.asFile.absolutePath,
      kotlinFileComplexityReport.get().asFile.absolutePath,
      kotlinFileComplexityState.get().asFile.absolutePath,
  )
  args(kotlinFileComplexitySources)
}
