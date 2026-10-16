import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("pets.jvm")
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
  implementation(libs.kotlinpoet)
  implementation(kotlin("reflect"))

  testImplementation(kotlin("test-junit5"))
  testImplementation(project(":tfm-canon"))
  testImplementation(project(":catalog"))
  testRuntimeOnly(libs.junit.platform.launcher)
}

val generatedCanonDirectory = layout.buildDirectory.dir("generated/canonCompilationTest")
val generateCanonCompilationTest by
    tasks.registering(JavaExec::class) {
      group = "verification"
      description = "Generates Kotlin for every class in the full Canon vocabulary."
      classpath = sourceSets.test.get().runtimeClasspath
      mainClass.set("dev.martianzoo.codegen.GenerateCanonCompilationTestKt")
      outputs.dir(generatedCanonDirectory)
      args(generatedCanonDirectory.get().asFile.absolutePath)
    }

val generatedCanon by sourceSets.creating {
  kotlin.srcDir(generateCanonCompilationTest)
}

configurations[generatedCanon.implementationConfigurationName].extendsFrom(
    configurations.implementation.get()
)

tasks.named<KotlinCompile>(generatedCanon.getCompileTaskName("kotlin")) {
  // This check replaces a single generated file. Incremental compilation has reported duplicate
  // helper functions after merges; compile the complete file when its inputs change instead.
  incremental = false
}

tasks.named("test") { dependsOn(generatedCanon.classesTaskName) }

// Search consumes plain common Kotlin; reflection is used only by this build-time generator.
tasks.register<JavaExec>("generatePetsXml") {
  group = "build"
  description = "Generates the XML field visitor from Pets AST constructor properties."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass.set("dev.martianzoo.codegen.GeneratePetsXmlKt")
  val outputDirectory = layout.buildDirectory.dir("generated/petsXml")
  outputs.dir(outputDirectory)
  args(outputDirectory.get().asFile.absolutePath)
}
