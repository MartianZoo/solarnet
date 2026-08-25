plugins {
  id("solarnet.jvm")
  application
}

dependencies {
  implementation(project(":canon"))
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
  inputs.files(
      project(":canon").fileTree("src/commonMain/resources/canon/bundles") {
        include("**/classes.pets")
      }
  )
  args(project(":canon").file("src/commonMain/resources").absolutePath)
}

tasks.register<JavaExec>("generateCardPets") {
  group = "build"
  description = "Generates inspectable Pets declarations for every canonical card bundle."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tools.GenerateCardPetsKt")
  inputs.files(
      project(":canon").fileTree("src/commonMain/resources/canon/bundles") {
        include("**/cards.json5")
      }
  )
  args(project(":canon").file("src/commonMain/resources").absolutePath)
}
