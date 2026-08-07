import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("solarnet.kotlin-base")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
  add("testImplementation", platform(libs.findLibrary("junit-bom").get()))
  add("testImplementation", kotlin("test-junit5"))
  add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
}
