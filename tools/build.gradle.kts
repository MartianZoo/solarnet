plugins {
  id("solarnet.jvm")
  application
}

dependencies {
  implementation(project(":canon"))
  implementation(project(":pets"))
  implementation(libs.kotlinpoet)

  testImplementation(kotlin("test-junit5"))
  testRuntimeOnly(libs.junit.platform.launcher)
}

application {
  mainClass.set("dev.martianzoo.tools.SoloPlacementKt")
  applicationName = "solo-placement"
}

val runPetsTypeGenerator =
    tasks.register<JavaExec>("runPetsTypeGenerator") {
      group = "application"
      description = "Generates Kotlin declarations for the resolved canonical Pets class table."
      classpath = sourceSets.main.get().runtimeClasspath
      mainClass.set("dev.martianzoo.tools.PetsTypeGeneratorKt")
    }

val petsTypeGeneratorStartScripts =
    tasks.register<CreateStartScripts>("petsTypeGeneratorStartScripts") {
      applicationName = "pets-type-generator"
      mainClass.set("dev.martianzoo.tools.PetsTypeGeneratorKt")
      outputDir = layout.buildDirectory.dir("pets-type-generator-scripts").get().asFile
      classpath =
          files(tasks.named<Jar>("jar").flatMap { it.archiveFile }) +
              configurations.runtimeClasspath.get()
    }

distributions {
  main {
    contents {
      from(petsTypeGeneratorStartScripts) { into("bin") }
    }
  }
}

val generatedPetsTypesDirectory = layout.buildDirectory.dir("generated/pets-types")
val generatePetsTypesForCompilation =
    tasks.register<JavaExec>("generatePetsTypesForCompilation") {
      group = "verification"
      description = "Generates the canonical Pets hierarchy used by the compile verification."
      dependsOn(tasks.named("classes"))
      classpath = sourceSets.main.get().runtimeClasspath
      mainClass.set("dev.martianzoo.tools.PetsTypeGeneratorKt")
      val output = generatedPetsTypesDirectory.map { it.file("CanonicalPetsTypes.kt") }
      outputs.file(output)
      args("--output", output.get().asFile.absolutePath)
    }

val generatedPetsTypes by sourceSets.creating

kotlin.sourceSets.named(generatedPetsTypes.name) {
  kotlin.srcDir(generatedPetsTypesDirectory)
}

tasks.named("compileGeneratedPetsTypesKotlin") {
  dependsOn(generatePetsTypesForCompilation)
}

tasks.named("check") {
  dependsOn(tasks.named(generatedPetsTypes.classesTaskName))
}
