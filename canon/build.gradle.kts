import java.net.URI
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("org.jetbrains.dokka")
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

val copyCanonResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn("jsProcessResources")
      from(layout.buildDirectory.dir("processedResources/js/main"))
      into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-canon-test"))
    }

val copyPetsResourcesForKarma by
    tasks.registering(Copy::class) {
      dependsOn(":pets:jsProcessResources")
      from(project(":pets").layout.buildDirectory.dir("processedResources/js/main/pets"))
      into(rootProject.layout.buildDirectory.dir("js/packages/solarnet-canon-test/pets"))
    }

kotlin {
  jvm()
  js(IR) {
    browser()
  }

  sourceSets {
    commonMain {
      resources.srcDir(generatedCanonResources)
      dependencies {
        implementation(project(":pets"))
      }
    }
    commonTest {
      dependencies {
        implementation(kotlin("test"))
        implementation("io.kotest:kotest-assertions-core:6.1.11")
      }
    }
  }
}

tasks.named("jsBrowserTest") {
  dependsOn(copyCanonResourcesForKarma)
  dependsOn(copyPetsResourcesForKarma)
}

tasks
    .matching { it.name.endsWith("ProcessResources") }
    .configureEach {
      dependsOn(generateCanonResourceIndex)
    }

dokka {
  dokkaSourceSets {
    configureEach {
      sourceLink {
        localDirectory.set(file("src"))
        remoteUrl.set(URI("https://github.com/MartianZoo/solarnet/tree/main/canon/src"))
        remoteLineSuffix.set("#L")
      }
    }
    named("commonMain") {
      samples.from("src/commonMain/kotlin/dev/martianzoo/tfm/canon/samples.kt")
    }
  }
}
