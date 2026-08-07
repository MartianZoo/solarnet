plugins {
  id("solarnet.jvm")
  application
}

dependencies {
  implementation(project(":canon"))
  implementation(project(":pets"))
}

application {
  mainClass.set("dev.martianzoo.tools.SoloPlacementKt")
  applicationName = "solo-placement"
}
