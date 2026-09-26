package dev.martianzoo.tfm.text

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.fake.FakeCanon
import java.io.File

private object EnglishCardTextCurrentGenerator {
  @JvmStatic
  fun main(args: Array<String>) {
    require(args.size == 2)
    val output = File(args[0])
    val refusalOutput = File(args[1])
    val catalog = TfmCatalog.compose(Canon, FakeCanon)
    val english = English(catalog.classTable, TerraformingMarsDescribers.descriptions)
    val published =
        EnglishCardTextData.parse(readEnglishCardText("english-published-wording-evidence.tsv"))
    val orderedCards = published.keys.map(catalog.classTable::getClass)
    val renderedCards = orderedCards.map { card -> card to english.renderCard(card) }
    val rows = renderedCards.map { (card, rendering) ->
      listOf(
              card.className.toString(),
              published.getValue(card.className).englishName,
              rendering.bottom,
              rendering.top,
          )
          .also { columns -> require(columns.none { '\t' in it || '\n' in it || '\r' in it }) }
          .joinToString("\t")
          .trimEnd('\t')
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
