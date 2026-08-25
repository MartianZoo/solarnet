package dev.martianzoo.tools

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.CardDefinition
import java.nio.file.Files
import java.nio.file.Path

private fun renderCard(card: CardDefinition): String = buildString {
  val declaration = card.asClassDeclaration
  card.replaces?.let { appendLine("// replaces: $it") }
  append("CLASS ${declaration.className}")
  if (declaration.dependencies.isNotEmpty()) {
    declaration.dependencies.joinTo(this, ", ", "<", ">", transform = Expression::toString)
  }
  if (declaration.supertypes.isNotEmpty()) {
    declaration.supertypes.sortedBy(Expression::toString).joinTo(this, ", ", " : ")
  }

  val sections =
      listOf(
              declaration.invariants.sortedBy(Requirement::toString).map { "HAS $it" },
              declaration.properties.map { (name, value) -> "$name = $value" },
              buildList {
                if (card.tags.isNotEmpty()) add(declaration.effects.first().toString())
                card.authoredImmediateSource?.let { add("This: $it") }
                addAll(card.authoredEffectSources)
              },
              card.authoredActionSources,
          )
          .filter(List<String>::isNotEmpty)
  if (sections.isNotEmpty()) {
    sections
        .map { lines -> lines.joinToString("\n  ") }
        .joinTo(this, separator = "\n\n  ", prefix = " {\n  ", postfix = "\n}")
  }
}

internal fun canonicalCardPetsFiles(): Map<String, String> = buildMap {
  Canon.bundles.forEach { bundle ->
    val cards = bundle.cardDefinitions.toList()
    if (cards.isNotEmpty()) {
      val supportingDeclarations =
          cards.flatMap(CardDefinition::authoredSupportingClasses).distinct()
      val source = buildString {
        appendLine("// Generated from cards.json5.")
        append(cards.joinToString("\n\n", transform = ::renderCard))
        if (supportingDeclarations.isNotEmpty()) {
          appendLine("\n\n// Supporting declarations contributed by cards in this bundle.")
          append(
              supportingDeclarations.joinToString("\n\n", transform = ClassDeclaration::toString)
          )
        }
        append('\n')
      }
      val expectedNames =
          cards.map(CardDefinition::className) +
              cards
                  .flatMap { card -> card.extraClasses + card.authoredSupportingClasses }
                  .map(ClassDeclaration::className)
      check(
          parseClasses(source).map(ClassDeclaration::className).toSet() == expectedNames.toSet()
      ) {
        "Generated card Pets changed declaration identities in ${bundle.bundleName}"
      }
      put("canon/bundles/${bundle.bundleName}/cards.pets", source)
    }
  }
}

public fun main(args: Array<String>) {
  require(args.size == 1) { "Usage: generateCardPets <output-directory>" }
  val output = Path.of(args.single())
  val files = canonicalCardPetsFiles()
  files.forEach { (relativePath, source) ->
    val file = output.resolve(relativePath)
    Files.createDirectories(file.parent)
    Files.writeString(file, source)
  }
  println("Wrote ${files.size} card Pets files under $output")
}
