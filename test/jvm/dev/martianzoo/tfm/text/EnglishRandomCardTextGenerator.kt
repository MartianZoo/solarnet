package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.Class
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.cardActions
import dev.martianzoo.tfm.canon.cardEffects
import dev.martianzoo.tfm.canon.cardImmediate
import dev.martianzoo.tfm.canon.cardRequirement
import java.io.File

/** Renders a saved random-card PETS report without coupling its generator to this module. */
private object EnglishRandomCardTextGenerator {
  @JvmStatic
  fun main(args: Array<String>) {
    require(args.size in 2..3) {
      "usage: EnglishRandomCardTextGenerator input-file output-file [comparison-output-file]"
    }
    val input = File(args[0])
    val declarations = readDeclarations(input)
    val catalog = catalogWith(declarations)
    val english = English(TerraformingMarsDescribers.descriptions)
    val describers = Describers(TerraformingMarsDescribers.descriptions)
    val results = declarations.map { declaration ->
      render(declaration, catalog.card(declaration.className), english, describers)
    }

    File(args[1]).writeText(report(input, results))
    args.getOrNull(2)?.let { File(it).writeText(comparisonReport(results)) }
  }

  private fun readDeclarations(input: File): List<ClassDeclaration> {
    val petsSource =
        input
            .readLines()
            .filterNot { line ->
              line.startsWith("Random card seed:") || line.matches(Regex("=== Card \\d+ ==="))
            }
            .joinToString("\n")
    return parseClasses(petsSource)
  }

  private fun catalogWith(declarations: List<ClassDeclaration>): TfmCatalog {
    val additions =
        object : TfmCatalog() {
          override val explicitClassDeclarations: Set<ClassDeclaration> = declarations.toSet()
        }
    return TfmCatalog.compose(Canon, additions)
  }

  private fun render(
      declaration: ClassDeclaration,
      card: Class,
      english: English,
      describers: Describers,
  ): CardResult {
    val topPets = topPets(card, describers)
    val bottomPets = bottomPets(card, describers)
    val complete = runCatching { english.renderCard(card) }
    return complete.fold(
        onSuccess = { rendering ->
          CardResult(
              declaration,
              RegionResult(rendering.top),
              RegionResult(rendering.bottom),
              rendering.unresolved,
              topPets = topPets,
              bottomPets = bottomPets,
          )
        },
        onFailure = { failure ->
          CardResult(
              declaration,
              renderRegion { english.topText(card) },
              renderRegion { english.bottomText(card) },
              emptyList(),
              error(failure),
              topPets,
              bottomPets,
          )
        },
    )
  }

  private fun renderRegion(block: () -> String): RegionResult =
      runCatching(block)
          .fold(
              onSuccess = ::RegionResult,
              onFailure = { RegionResult(error = error(it)) },
          )

  private fun error(failure: Throwable): String =
      "${failure.javaClass.simpleName}: ${failure.message.orEmpty().replace('\n', ' ')}"

  private fun topPets(card: Class, describers: Describers): List<String> {
    val effects = cardEffects(card)
    val resourceValueEffects = renderCardResourceValueEffects(effects, describers).first
    return cardActions(card).map { it.toString() } +
        effects
            .filterNot { it in resourceValueEffects || isEndEffect(it, describers) }
            .map { it.toString() }
  }

  private fun bottomPets(card: Class, describers: Describers): List<String> =
      listOfNotNull(
          cardRequirement(card)?.let { "requirement = HAS \"$it\"" },
          cardImmediate(card)?.let { "This: $it" },
      ) +
          cardEffects(card)
              .filter { isEndEffect(it, describers) }
              .filterNot { isUnconditionalFixedScore(it, describers) }
              .map { it.toString() }

  private fun report(input: File, results: List<CardResult>): String = buildString {
    val unresolved = results.flatMap(CardResult::unresolved)
    val errored = results.count { it.top.error != null || it.bottom.error != null }
    appendLine("Random-card English rendering")
    appendLine("Input: ${input.path}")
    appendLine("Cards: ${results.size}")
    appendLine("Cards with region errors: $errored")
    appendLine("Cards with unresolved PETS: ${results.count { it.unresolved.isNotEmpty() }}")
    appendLine("Unresolved PETS occurrences: ${unresolved.size}")
    unresolved
        .groupingBy { it.reason }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .forEach { (reason, count) -> appendLine("  $count $reason") }

    results.forEachIndexed { index, result ->
      appendLine()
      appendLine("=== Card ${index + 1}: ${result.declaration.className} ===")
      appendLine("PETS:")
      appendLine(result.declaration)
      appendLine("TOP:")
      appendLine(result.top.display())
      appendLine("BOTTOM:")
      appendLine(result.bottom.display())
      if (result.unresolved.isNotEmpty()) {
        appendLine("UNRESOLVED:")
        result.unresolved.forEach { appendLine("- ${it.reason}: ${it.node}") }
      }
      result.completeError?.let {
        appendLine("COMPLETE RENDER ERROR:")
        appendLine(it)
      }
    }
  }

  private fun comparisonReport(results: List<CardResult>): String =
      results
          .mapIndexedNotNull { index, result ->
            val regions =
                if (index % 2 == 0) {
                  listOf(
                      Comparison("BOTTOM TEXT", result.bottomPets, result.bottom),
                      Comparison("TOP TEXT", result.topPets, result.top),
                  )
                } else {
                  listOf(
                      Comparison("TOP TEXT", result.topPets, result.top),
                      Comparison("BOTTOM TEXT", result.bottomPets, result.bottom),
                  )
                }
            regions.firstOrNull(Comparison::clean)?.let { comparison ->
              buildString {
                appendLine(result.declaration.className)
                appendLine()
                appendLine("PETS:")
                comparison.pets.forEach(::appendLine)
                appendLine()
                appendLine("${comparison.label}:")
                appendLine(comparison.rendering.text)
              }
            }
          }
          .joinToString("\n\n")

  private data class RegionResult(
      val text: String? = null,
      val error: String? = null,
  ) {
    fun display(): String = error?.let { "ERROR: $it" } ?: text?.ifEmpty { "(empty)" }.orEmpty()
  }

  private data class CardResult(
      val declaration: ClassDeclaration,
      val top: RegionResult,
      val bottom: RegionResult,
      val unresolved: List<Unresolved>,
      val completeError: String? = null,
      val topPets: List<String>,
      val bottomPets: List<String>,
  )

  private data class Comparison(
      val label: String,
      val pets: List<String>,
      val rendering: RegionResult,
  ) {
    fun clean(): Boolean =
        pets.isNotEmpty() &&
            rendering.error == null &&
            !rendering.text.isNullOrEmpty() &&
            '[' !in rendering.text
  }
}
