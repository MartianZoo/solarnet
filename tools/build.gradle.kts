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

val generatedMapPets = layout.buildDirectory.dir("generated/mapPets")

tasks.register<JavaExec>("generateMapPets") {
  group = "build"
  description = "Generates Pets declarations for every canonical Mars map and area."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tools.GenerateMapPetsKt")
  inputs.files(
      project(":canon").fileTree("src/commonMain/resources/canon/bundles") {
        include("**/*maps.json5")
      }
  )
  outputs.dir(generatedMapPets)
  args(generatedMapPets.get().asFile.absolutePath, true)
}
