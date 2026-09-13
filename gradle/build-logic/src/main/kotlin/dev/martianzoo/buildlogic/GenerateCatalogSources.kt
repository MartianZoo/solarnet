package dev.martianzoo.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Embeds a catalog's non-Kotlin source files in generated common Kotlin source. */
@CacheableTask
public abstract class GenerateCatalogSources : DefaultTask() {
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  public abstract val inputDirectory: DirectoryProperty

  @get:Input public abstract val logicalPrefix: Property<String>

  @get:Input public abstract val packageName: Property<String>

  @get:Input public abstract val sourceName: Property<String>

  /** Optional basename filter for catalogs stored alongside other authored bundle files. */
  @get:Input public abstract val includedFileNames: SetProperty<String>

  @get:OutputDirectory public abstract val outputDirectory: DirectoryProperty

  init {
    includedFileNames.convention(emptySet())
  }

  @TaskAction
  public fun generate() {
    val input = inputDirectory.get().asFile
    val prefix = logicalPrefix.get().trim('/')
    val includedNames = includedFileNames.get()
    require(prefix.isNotEmpty()) { "Catalog logical prefix must not be empty" }
    val resourcesByBundle =
        input
            .walkTopDown()
            .filter {
              it.isFile &&
                  it.extension != "kt" &&
                  (includedNames.isEmpty() || it.name in includedNames)
            }
            .map { file ->
              val relativePath = file.relativeTo(input).invariantSeparatorsPath
              val segments = relativePath.split('/')
              require(segments.size >= 2) {
                "Catalog source is not inside a bundle directory: $relativePath"
              }
              val bundle = segments.first()
              require(bundle.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                "Catalog bundle is not a Kotlin identifier: $bundle"
              }
              val logicalRelativePath =
                  if (segments.size == 2 && segments.last() == "en.json5") {
                    "$bundle/language/en.json5"
                  } else {
                    relativePath
                  }
              bundle to ("$prefix/$logicalRelativePath" to file.readText())
            }
            .toList()
            .groupBy({ it.first }, { it.second })
            .toSortedMap()

    val output = outputDirectory.get().asFile
    output.deleteRecursively()
    val packageDirectory = output.resolve(packageName.get().replace('.', '/'))
    packageDirectory.mkdirs()

    resourcesByBundle.forEach { (bundle, resources) ->
      packageDirectory
          .resolve("${bundle}${sourceName.get()}Sources.kt")
          .writeText(renderBundle(bundle, resources.sortedBy(Pair<String, String>::first)))
    }
    packageDirectory
        .resolve("Generated${sourceName.get()}Resources.kt")
        .writeText(renderRegistry(resourcesByBundle.keys))
  }

  private fun renderBundle(
      bundle: String,
      resources: List<Pair<String, String>>,
  ): String = buildString {
    appendLine("package ${packageName.get()}")
    appendLine()
    appendLine("internal object ${bundle}${sourceName.get()}Sources {")
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

  private fun renderRegistry(bundles: Set<String>): String = buildString {
    appendLine("package ${packageName.get()}")
    appendLine()
    appendLine("internal object Generated${sourceName.get()}Resources {")
    appendLine("  private val resources: Map<String, String> =")
    appendLine("      buildMap {")
    bundles.forEach { bundle ->
      appendLine("        putAll(${bundle}${sourceName.get()}Sources.resources)")
    }
    appendLine("      }")
    appendLine()
    appendLine("  internal val filenames: Set<String>")
    appendLine("    get() = resources.keys")
    appendLine()
    appendLine("  internal fun read(filename: String): String =")
    appendLine("      resources[filename] ?: error(\"Unknown catalog resource: \$filename\")")
    appendLine("}")
  }

  private fun renderContents(contents: String): String {
    val chunks = contents.chunked(STRING_CHUNK_SIZE)
    if (chunks.size <= 1) return "              ${contents.asKotlinLiteral()}"
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
            if (char.code in 0x20..0x7e) append(char)
            else {
              append("\\u")
              repeat(4 - char.code.toString(16).length) { append('0') }
              append(char.code.toString(16).uppercase())
            }
      }
    }
    append('"')
  }

  private companion object {
    private const val STRING_CHUNK_SIZE = 8_000
  }
}
