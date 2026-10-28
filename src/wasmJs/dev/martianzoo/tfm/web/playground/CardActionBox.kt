package dev.martianzoo.tfm.web.playground

import dev.martianzoo.tfm.web.playground.resources.Res

/** An authored action's printed rectangle, measured in pixels of its source card image. */
internal data class CardActionBox(
    val card: String,
    val slot: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val imageWidth: Int,
    val imageHeight: Int,
)

/** The local action-box survey, copied into the deployed Playground resources. */
internal suspend fun loadCardActionBoxes(): Map<String, List<CardActionBox>> =
    Res.readBytes("files/action-bounds.tsv")
        .decodeToString()
        .lineSequence()
        .drop(1)
        .filter(String::isNotBlank)
        .map { line ->
          val fields = line.split('\t')
          require(fields.size >= 9) { "Invalid card action box: $line" }
          CardActionBox(
              card = fields[1],
              slot = fields[2].toInt(),
              x = fields[3].toInt(),
              y = fields[4].toInt(),
              width = fields[5].toInt(),
              height = fields[6].toInt(),
              imageWidth = fields[7].toInt(),
              imageHeight = fields[8].toInt(),
          )
        }
        .toList()
        .groupBy(CardActionBox::card)
