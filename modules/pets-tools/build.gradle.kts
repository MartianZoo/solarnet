plugins {
  id("solarnet.jvm")
  application
}

val petsToolsSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tfm/petstools")
val canonSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/canon")

kotlin {
  sourceSets {
    main { kotlin.setSrcDirs(listOf(petsToolsSourceDirectory)) }
    test {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tfm/petstools"),
              rootProject.layout.projectDirectory.dir(
                  "test/common/dev/martianzoo/pets/testsupport"
              ),
          )
      )
      kotlin.exclude("PetGenerator.kt", "testHelpers.kt")
    }
  }
}

dependencies {
  implementation(project(":catalog"))
  implementation(project(":pets"))
  implementation(project(":tfm-canon"))
  testImplementation(libs.kotest.assertions.core)
}

application {
  mainClass.set("dev.martianzoo.tfm.petstools.SoloPlacementKt")
  applicationName = "solo-placement"
}

tasks.register<JavaExec>("standardResourceMonotonicityReport") {
  group = "application"
  description = "Reports declarative threats to solo resource and production monotonicity."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.petstools.StandardResourceMonotonicityReportKt")
}

val randomCardCount = providers.gradleProperty("randomCardCount").orElse("12")
val randomCardSeed = providers.gradleProperty("randomCardSeed")
val randomCardOutput = providers.gradleProperty("randomCardOutput")

tasks.register<JavaExec>("sampleRandomCards") {
  group = "verification"
  description = "Prints or writes randomly generated project cards as raw Pets."
  dependsOn(tasks.named("testClasses"))
  classpath = sourceSets.test.get().runtimeClasspath
  mainClass = "dev.martianzoo.tfm.petstools.randomcards.RandomCardGenerator"
  args(randomCardCount.get())
  randomCardSeed.orNull?.let { args(it) }
  randomCardOutput.orNull?.let {
    require(randomCardSeed.isPresent) { "randomCardOutput requires randomCardSeed" }
    args(it)
  }
}

tasks.register<JavaExec>("regenerateMapAreas") {
  group = "build"
  description = "Regenerates canonical map-area declarations from diagrams in Pets comments."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.petstools.RegenerateMapAreasKt")
  inputs.files(canonSourceDirectory.asFileTree.matching { include("**/*.pets") })
  args(canonSourceDirectory.asFile.absolutePath)
}
