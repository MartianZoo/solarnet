import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

plugins {
  id("solarnet.kmp-jvm-js")
}

val canonResourceDirectory = layout.projectDirectory.dir("src/commonMain/resources/canon")
val generatedCanonResources = layout.buildDirectory.dir("generated/canonResourceIndex")

abstract class GenerateResourceIndex : DefaultTask() {
  @get:InputDirectory abstract val resourceDirectory: DirectoryProperty
  @get:OutputFile abstract val indexFile: RegularFileProperty

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
    val output = indexFile.get().asFile
    output.parentFile.mkdirs()
    output.writeText(paths.joinToString(separator = "\n", postfix = "\n"))
  }
}

val generateCanonResourceIndex by
    tasks.registering(GenerateResourceIndex::class) {
      resourceDirectory.set(canonResourceDirectory)
      indexFile.set(generatedCanonResources.map { it.file("canon/resource-index.txt") })
    }

kotlin {
  sourceSets {
    commonMain {
      resources.srcDir(generatedCanonResources)
      dependencies {
        implementation(project(":pets"))
      }
    }
    commonTest {
      dependencies { implementation(libs.kotest.assertions.core) }
    }
  }
}

tasks
    .matching { it.name.endsWith("ProcessResources") }
    .configureEach {
      dependsOn(generateCanonResourceIndex)
    }

dokka {
  dokkaSourceSets {
    named("commonMain") {
      samples.from("src/commonMain/kotlin/dev/martianzoo/tfm/canon/samples.kt")
    }
  }
}
