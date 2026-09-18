import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.PathSensitivity

plugins {
  id("solarnet.jvm")
  application
}

val kotlinFileComplexityAnalyzer by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = true
}

val toolsSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tfm/tools")
val canonSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/canon")

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
  implementation(project(":agent"))
  implementation(project(":tfm-canon"))
  implementation(project(":tfm-fake"))
  implementation(project(":engine"))
  implementation(project(":pets"))
  implementation(project(":state"))
  implementation(project(":tfm-engine"))
  kotlinFileComplexityAnalyzer(libs.detekt.metrics)
  testRuntimeOnly(libs.detekt.metrics)
}

application {
  mainClass.set("dev.martianzoo.tfm.tools.SoloPlacementKt")
  applicationName = "solo-placement"
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

tasks.register<JavaExec>("standardResourceMonotonicityReport") {
  group = "application"
  description = "Reports declarative threats to solo resource and production monotonicity."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.tools.StandardResourceMonotonicityReportKt")
}

val eventLogDumpOutput =
    rootProject.layout.projectDirectory.file(
        "_local/eventlogs/three-player-all-expansions-eventlog.tsv"
    )
val soloEventLogDumpOutput =
    rootProject.layout.projectDirectory.file("_local/eventlogs/solo-all-expansions-eventlog.tsv")
val otbGame20260828EventLogDumpOutput =
    rootProject.layout.projectDirectory.file("_local/eventlogs/otb-game-20260828-eventlog.tsv")
val replayEventLogsDirectory =
    project(":tfm-tests").layout.buildDirectory.dir("generated/replay-event-logs")

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
  dependsOn(":tfm-tests:jvmTest")
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.tools.DumpEventlogKt")
  args(
      replayEventLogsDirectory
          .map { it.file("OtbGame20260828Test.json").asFile.absolutePath }
          .get(),
      otbGame20260828EventLogDumpOutput.asFile.absolutePath,
  )
}

tasks.register<JavaExec>("regenerateMapAreas") {
  group = "build"
  description = "Regenerates canonical map-area declarations from diagrams in Pets comments."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.tools.RegenerateMapAreasKt")
  inputs.files(canonSourceDirectory.asFileTree.matching { include("**/*.pets") })
  args(canonSourceDirectory.asFile.absolutePath)
}

val kotlinFileComplexitySources =
    rootProject.layout.projectDirectory.asFileTree.matching {
      include("src/**/*.kt")
      include("test/**/*.kt")
      exclude("src/**/dev/martianzoo/tfm/tools/**")
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
