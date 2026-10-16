pluginManagement { includeBuild("gradle/build-logic") }

rootProject.name = "pets"

val modules =
    listOf(
        "pets",
        "catalog",
        "tfm-card-data",
        "tfm-compiler",
        "tfm-canon",
        "tfm-fake",
        "text",
        "almanac",
        "junkdrawer",
        "search",
        "codegen",
    )

include(*modules.toTypedArray())

modules.forEach { project(":$it").projectDir = file("modules/$it") }

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    exclusiveContent {
      forRepository { maven { url = uri("https://jitpack.io") } }
      filter { includeGroup("com.github.kevinb9n.better-parse") }
    }
  }
}
