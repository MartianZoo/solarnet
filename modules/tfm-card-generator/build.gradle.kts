plugins { id("solarnet.jvm") }

val generatorSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/tfm/cardgenerator")

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
  args(outputDirectory.get().asFile.absolutePath)
}
