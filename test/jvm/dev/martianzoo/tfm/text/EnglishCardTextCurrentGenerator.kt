package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Vocabulary.Companion.defaultEnglishDisplayName
import dev.martianzoo.tfm.canon.Canon
import java.io.File

private object EnglishCardTextCurrentGenerator {
  @JvmStatic
  fun main(args: Array<String>) {
    require(args.size == 2)
    val output = File(args[0])
    val refusalOutput = File(args[1])
    val goals = EnglishCardTextData.parse(readEnglishCardText("english-card-text-goals.tsv"))
    val english = English(TerraformingMarsDescribers.descriptions)
    val renderedCards = Canon.cards.map { card -> card to english.renderCard(card) }
    val rows = renderedCards.map { (card, rendering) ->
      listOf(
              card.className.toString(),
              goals[card.className]?.englishName ?: defaultEnglishDisplayName(card.className),
              rendering.bottom,
              rendering.top,
          )
          .also { columns -> require(columns.none { '\t' in it || '\n' in it || '\r' in it }) }
          .joinToString("\t")
          .removeSuffix("\t")
    }
    output.writeText(
        (listOf("class_name\tenglish_name\tbottom_text\ttop_text") + rows).joinToString(
            "\n",
            postfix = "\n",
        )
    )
    val refusals = renderedCards.flatMap { (card, rendering) ->
      rendering.unresolved.map { CardRefusal(card.className.toString(), it) }
    }
    val refusalRows =
        refusals
            .groupBy { it.unresolved.reason }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<RefusalReason, List<CardRefusal>>> { it.value.size }
                    .thenBy { it.key.name }
            )
            .map { (reason, entries) ->
              val examples =
                  entries.take(2).joinToString(" | ") { "${it.cardClass}: ${it.unresolved.node}" }
              listOf(entries.size, reason, examples).joinToString("\t")
            }
    refusalOutput.writeText(
        (listOf("count\treason\texamples") + refusalRows).joinToString("\n", postfix = "\n")
    )
  }
}

private data class CardRefusal(val cardClass: String, val unresolved: Unresolved)
