package dev.martianzoo.tfm.petstools

import dev.martianzoo.catalog.displayName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Class
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.cardCost
import dev.martianzoo.tfm.canon.soloTileAreas
import kotlin.system.exitProcess

internal data class Placement(
    val tile: Tile,
    val ordinal: Int,
    val area: AreaDefinition,
    val card: Class,
    val drawOrdinal: Int,
) {
  enum class Tile {
    CITY,
    GREENERY,
  }
}

internal fun calculateSoloPlacements(arguments: List<String>): List<Placement> {
  val compatibility = arguments.firstOrNull() == "--compatibility"
  val namesInOrder = if (compatibility) arguments.drop(1) else arguments
  require(namesInOrder.size == 5) {
    "usage: solo-placement [--compatibility] MAP CARD1 CARD2 CARD3 CARD4"
  }
  val names = namesInOrder.map(::cn)
  val requestedMap = names.first()
  val mapName =
      cn("${requestedMap}Map").takeIf { it in soloPlacementCatalog.allClassNames } ?: requestedMap
  val map = soloPlacementCatalog.marsMap(mapName)
  val cards = names.drop(1).map(soloPlacementCatalog::card)
  cards.forEach {
    require(cardBack(it)?.className == cn("ProjectCard")) {
      "${displayName(soloPlacementCatalog, it.className)} is not a project card"
    }
    require(!compatibility || cardCost(it) > 0) {
      "${displayName(soloPlacementCatalog, it.className)} has cost 0, which compatibility mode rejects"
    }
  }
  val areas = soloTileAreas(map, cards.map(::cardCost), compatibility)
  val drawOrder = if (compatibility) listOf(0, 1, 2, 3) else listOf(0, 2, 1, 3)
  return areas.mapIndexed { index, area ->
    val draw = drawOrder[index]
    Placement(
        if (index % 2 == 0) Placement.Tile.CITY else Placement.Tile.GREENERY,
        index / 2 + 1,
        area,
        cards[draw],
        draw + 1,
    )
  }
}

internal fun formatPlacements(placements: List<Placement>): String {
  val drawOrder = placements.sortedBy(Placement::drawOrdinal)
  return buildString {
    appendLine("Cards (draw order):")
    drawOrder.forEachIndexed { index, placement ->
      appendLine(
          "${index + 1}. ${displayName(soloPlacementCatalog, placement.card.className)}: " +
              cardCost(placement.card)
      )
    }
    appendLine("Placements:")
    placements.forEach { placement ->
      val label = placement.tile.name.lowercase().replaceFirstChar(Char::uppercase)
      appendLine(
          "$label ${placement.ordinal}: ${placement.area.className} " +
              "(${displayName(soloPlacementCatalog, placement.card.className)}, ${cardCost(placement.card)})"
      )
    }
  }
      .trimEnd()
}

private val soloPlacementCatalog: TfmCatalog = Canon

public fun main(args: Array<String>) {
  try {
    println(formatPlacements(calculateSoloPlacements(args.toList())))
  } catch (e: IllegalArgumentException) {
    System.err.println(e.message)
    exitProcess(2)
  }
}
