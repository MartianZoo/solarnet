plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.dokka")
  application
}

dependencies {
  implementation(project(":canon"))
  implementation(project(":pets"))

  testImplementation(kotlin("test-junit5"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin { explicitApi() }

application {
  mainClass.set("dev.martianzoo.tools.SoloPlacementKt")
  applicationName = "solo-placement"
}
