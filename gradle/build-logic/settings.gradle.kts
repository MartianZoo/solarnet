dependencyResolutionManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  versionCatalogs { create("libs") { from(files("../libs.versions.toml")) } }
}

rootProject.name = "solarnet-build-logic"
