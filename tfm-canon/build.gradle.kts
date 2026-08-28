plugins {
  id("solarnet.kmp-jvm-js")
  alias(libs.plugins.kotlin.serialization)
}

// Canon reads its data files by name at runtime, which the JS target cannot enumerate on its own,
// so ship a generated index of them alongside the data itself.
abstract class GenerateResourceIndex : DefaultTask() {
  @get:InputDirectory abstract val resourceDirectory: DirectoryProperty

  @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun generate() {
    val directory = resourceDirectory.get().asFile
    val paths =
        directory
            .walkTopDown()
            .filter(File::isFile)
            .map { it.relativeTo(directory).invariantSeparatorsPath }
            .sorted()
            .toList()
    val output = outputDirectory.get().file("canon/resource-index.txt").asFile
    output.parentFile.mkdirs()
    output.writeText(paths.joinToString(separator = "\n", postfix = "\n"))
  }
}

val generateCanonResourceIndex by
    tasks.registering(GenerateResourceIndex::class) {
      resourceDirectory.set(layout.projectDirectory.dir("src/commonMain/resources/canon"))
      outputDirectory.set(layout.buildDirectory.dir("generated/canonResourceIndex"))
    }

kotlin {
  sourceSets {
    commonMain {
      // Registering the task itself as a source directory is what makes every `processResources`
      // task depend on it.
      resources.srcDir(generateCanonResourceIndex)
      dependencies {
        implementation(libs.kotlinx.serialization.json)
        implementation(project(":pets"))
      }
    }
    commonTest { dependencies { implementation(libs.kotest.assertions.core) } }
  }
}
