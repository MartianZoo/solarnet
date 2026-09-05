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

val toolsSourceDirectory = rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tools")
val canonSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/canon")

kotlin {
  sourceSets {
    main { kotlin.setSrcDirs(listOf(toolsSourceDirectory)) }
    test {
      kotlin.setSrcDirs(
          listOf(rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tools"))
      )
    }
  }
}

dependencies {
  implementation(project(":tfm-canon"))
  implementation(project(":engine"))
  implementation(project(":pets"))
  kotlinFileComplexityAnalyzer(libs.detekt.metrics)
  testRuntimeOnly(libs.detekt.metrics)
}

application {
  mainClass.set("dev.martianzoo.tools.SoloPlacementKt")
  applicationName = "solo-placement"
}

tasks.register<JavaExec>("typeStructureReport") {
  group = "application"
  description = "Reports encoding-relevant type statistics for an all-expansions five-player game."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tools.TypeStructureReportKt")
}

tasks.register<JavaExec>("standardResourceMonotonicityReport") {
  group = "application"
  description = "Reports declarative threats to solo resource and production monotonicity."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tools.StandardResourceMonotonicityReportKt")
}

tasks.register<JavaExec>("regenerateMapAreas") {
  group = "build"
  description = "Regenerates canonical map-area declarations from diagrams in Pets comments."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tools.RegenerateMapAreasKt")
  inputs.files(canonSourceDirectory.asFileTree.matching { include("*/classes.pets") })
  args(canonSourceDirectory.asFile.absolutePath)
}

val kotlinFileComplexitySources =
    rootProject.layout.projectDirectory.asFileTree.matching {
      include("src/**/*.kt")
      include("test/**/*.kt")
      exclude("src/**/dev/martianzoo/tools/**")
      exclude("test/**/dev/martianzoo/tools/**")
      exclude("test/**/dev/martianzoo/benchmarks/**")
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
  mainClass.set("dev.martianzoo.tools.KotlinFileComplexityKt")
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
