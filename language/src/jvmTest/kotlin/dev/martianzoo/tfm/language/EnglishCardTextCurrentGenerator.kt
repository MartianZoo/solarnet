package dev.martianzoo.tfm.language

import dev.martianzoo.pets.Vocabulary.Companion.defaultEnglishDisplayName
import dev.martianzoo.tfm.canon.Canon
import java.io.File

internal object EnglishCardTextCurrentGenerator {
  @JvmStatic
  fun main(args: Array<String>) {
    val output = File(args.single())
    val goals = EnglishCardTextData.parse(readEnglishCardText("english-card-text-goals.tsv"))
    val english = English(TerraformingMarsDescribers.descriptions)
    val rows =
        Canon.cardDefinitions.map { card ->
          listOf(
                  card.className.toString(),
                  goals[card.className]?.englishName ?: defaultEnglishDisplayName(card.className),
                  english.bottomText(card),
                  english.topText(card),
              )
              .also { columns ->
                require(columns.none { '\t' in it || '\n' in it || '\r' in it })
              }
              .joinToString("\t")
              .trimEnd('\t')
        }
    output.writeText(
        (listOf("class_name\tenglish_name\tbottom_text\ttop_text") + rows).joinToString(
            "\n",
            postfix = "\n",
        )
    )
  }
}
