plugins {
  id("solarnet.jvm")
  application
}

val codegenSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/jvm/dev/martianzoo/codegen")
val codegenTestDirectory =
    rootProject.layout.projectDirectory.dir("test/jvm/dev/martianzoo/codegen")

kotlin {
  sourceSets {
    main { kotlin.setSrcDirs(listOf(codegenSourceDirectory)) }
    test { kotlin.setSrcDirs(listOf(codegenTestDirectory)) }
  }
}

dependencies {
  implementation(project(":pets"))
  implementation(project(":tfm-canon"))
  implementation(libs.kotlinpoet)

  testImplementation(kotlin("test-junit5"))
  testRuntimeOnly(libs.junit.platform.launcher)
}

application {
  mainClass.set("dev.martianzoo.codegen.PetsTypeGeneratorKt")
  applicationName = "pets-type-generator"
}

val generatedPetsTypesDirectory = layout.buildDirectory.dir("generated/pets-types")
val canonicalPetsSourceDirectory =
    rootProject.layout.projectDirectory.dir("src/common/dev/martianzoo/tfm/canon")

val generatePetsTypes =
    tasks.register<JavaExec>("generatePetsTypes") {
      group = "build"
      description = "Generates the canonical Pets Kotlin hierarchy."
      dependsOn(tasks.named("classes"))
      classpath = sourceSets.main.get().runtimeClasspath
      mainClass.set("dev.martianzoo.codegen.PetsTypeGeneratorKt")
      inputs.files(canonicalPetsSourceDirectory.asFileTree.matching { include("**/*.pets") })
      outputs.dir(generatedPetsTypesDirectory)
      args(
          "--output-dir",
          generatedPetsTypesDirectory.get().asFile.absolutePath,
      )
    }

tasks.register<JavaExec>("runPetsTypeGenerator") {
  group = "application"
  description = "Runs the canonical Pets Kotlin generator with caller-provided arguments."
  dependsOn(tasks.named("classes"))
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.codegen.PetsTypeGeneratorKt")
}
