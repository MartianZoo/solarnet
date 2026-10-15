plugins {
  id("solarnet.jvm")
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

tasks.named("test") { dependsOn(generatedCanon.classesTaskName) }
