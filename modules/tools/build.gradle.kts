plugins {
  id("solarnet.jvm")
  application
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
