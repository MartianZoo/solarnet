import org.gradle.api.tasks.PathSensitivity

plugins { id("solarnet.jvm") }

val generatorSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tfm/cardgenerator")
val authoredCardPetsDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/canon")

kotlin {
  sourceSets {
    main { kotlin.setSrcDirs(listOf(generatorSourceDirectory)) }
    test {
      kotlin.setSrcDirs(
          listOf(
              rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/tfm/cardgenerator")
          )
      )
    }
  }
}

dependencies {
  implementation(project(":pets"))
  implementation(project(":tfm-card-data"))
}

tasks.register<JavaExec>("generateCardPets") {
  group = "build"
  description = "Generates canonical card declarations into the build directory."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.tfm.cardgenerator.GenerateCardPetsKt")
  val outputDirectory = layout.buildDirectory.dir("generated/cardPets")
  outputs.dir(outputDirectory)
  inputs
      .files(fileTree(authoredCardPetsDirectory) { include("*/cards.pets") })
      .withPathSensitivity(PathSensitivity.RELATIVE)
  args(outputDirectory.get().asFile.absolutePath, authoredCardPetsDirectory.asFile.absolutePath)
}
