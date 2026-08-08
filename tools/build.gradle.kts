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
