plugins {
  id("solarnet.jvm")
  application
}

dependencies {
  implementation(project(":canon"))
  implementation(project(":pets"))

  testImplementation(kotlin("test-junit5"))
  testRuntimeOnly(libs.junit.platform.launcher)
}

application {
  mainClass.set("dev.martianzoo.tools.SoloPlacementKt")
  applicationName = "solo-placement"
}
