plugins {
  id("solarnet.kmp-jvm-js")
  alias(libs.plugins.kotlin.serialization)
}

@CacheableTask
abstract class GenerateCanonSources : DefaultTask() {
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputDirectory: DirectoryProperty

  @get:Input abstract val logicalPrefix: Property<String>

  @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun generate() {
    val input = inputDirectory.get().asFile
    val prefix = logicalPrefix.get().trim('/')
    require(prefix.isNotEmpty()) { "Canon logical prefix must not be empty" }
    val resourcesByExpansion =
        input
            .walkTopDown()
            .filter { it.isFile && it.extension != "kt" }
            .map { file ->
              val relativePath = file.relativeTo(input).invariantSeparatorsPath
              val segments = relativePath.split('/')
              require(segments.size >= 2) {
                "Canon source is not inside an expansion directory: $relativePath"
              }
              val expansion = segments.first()
              require(expansion.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                "Canon expansion is not a Kotlin identifier: $expansion"
              }
              expansion to ("$prefix/$relativePath" to file.readText())
            }
            .toList()
            .groupBy({ it.first }, { it.second })
            .toSortedMap()

    val output = outputDirectory.get().asFile
    output.deleteRecursively()
    val packageDirectory = output.resolve(PACKAGE_PATH)
    packageDirectory.mkdirs()

    resourcesByExpansion.forEach { (expansion, resources) ->
      packageDirectory
          .resolve("${expansion}CanonSources.kt")
          .writeText(renderExpansion(expansion, resources.sortedBy(Pair<String, String>::first)))
    }
    packageDirectory
        .resolve("GeneratedCanonResources.kt")
        .writeText(renderRegistry(resourcesByExpansion.keys))
  }

  private fun renderExpansion(
      expansion: String,
      resources: List<Pair<String, String>>,
  ): String = buildString {
    appendLine("package $PACKAGE_NAME")
    appendLine()
    appendLine("internal object ${expansion}CanonSources {")
    appendLine("  internal val resources: Map<String, String> =")
    appendLine("      mapOf(")
    resources.forEach { (path, contents) ->
      appendLine("          ${path.asKotlinLiteral()} to")
      append(renderContents(contents))
      appendLine(",")
    }
    appendLine("      )")
    appendLine("}")
  }

  private fun renderRegistry(expansions: Set<String>): String = buildString {
    appendLine("package $PACKAGE_NAME")
    appendLine()
    appendLine("internal object GeneratedCanonResources {")
    appendLine("  private val resources: Map<String, String> =")
    appendLine("      buildMap {")
    expansions.forEach { expansion ->
      appendLine("        putAll(${expansion}CanonSources.resources)")
    }
    appendLine("      }")
    appendLine()
    appendLine("  internal val filenames: Set<String>")
    appendLine("    get() = resources.keys")
    appendLine()
    appendLine("  internal fun read(filename: String): String =")
    appendLine("      resources[filename] ?: error(\"Unknown canon resource: \$filename\")")
    appendLine("}")
  }

  private fun renderContents(contents: String): String {
    val chunks = contents.chunked(STRING_CHUNK_SIZE)
    if (chunks.size <= 1) {
      return "              ${contents.asKotlinLiteral()}"
    }
    return buildString {
      appendLine("              listOf(")
      chunks.forEach { chunk -> appendLine("                  ${chunk.asKotlinLiteral()},") }
      append("              ).joinToString(separator = \"\")")
    }
  }

  private fun String.asKotlinLiteral(): String = buildString {
    append('"')
    this@asKotlinLiteral.forEach { char ->
      when (char) {
        '\\' -> append("\\\\")
        '"' -> append("\\\"")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        '$' -> append('\\').append('$')
        else ->
            if (char.code in 0x20..0x7e) {
              append(char)
            } else {
              append("\\u")
              repeat(4 - char.code.toString(16).length) { append('0') }
              append(char.code.toString(16).uppercase())
            }
      }
    }
    append('"')
  }

  private companion object {
    private const val PACKAGE_NAME = "dev.martianzoo.tfm.canon"
    private const val PACKAGE_PATH = "dev/martianzoo/tfm/canon"
    private const val STRING_CHUNK_SIZE = 8_000
  }
}

val generateCanonSources by
    tasks.registering(GenerateCanonSources::class) {
      inputDirectory.set(layout.projectDirectory.dir("src/commonMain/resources/canon/bundles"))
      logicalPrefix.set("bundles")
      outputDirectory.set(layout.buildDirectory.dir("generated/canonSources"))
    }

kotlin {
  sourceSets {
    commonMain {
      kotlin.srcDir(generateCanonSources)
      dependencies {
        implementation(libs.kotlinx.serialization.json)
        api(project(":engine"))
        implementation(project(":pets"))
        implementation(project(":tfm-engine"))
      }
    }
    commonTest { dependencies { implementation(libs.kotest.assertions.core) } }
  }
}
