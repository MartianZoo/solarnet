package dev.martianzoo.tools

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import kotlin.system.exitProcess

internal enum class SoloTile {
  CITY,
  GREENERY,
}

internal enum class PlacementMode {
  COUNT_FROM_ZERO,
  COMPATIBILITY,
}

internal data class Placement(
    val tile: SoloTile,
    val ordinal: Int,
    val area: AreaDefinition,
    val card: CardDefinition,
)

internal class SoloPlacementCalculator(
    private val map: MarsMapDefinition,
    private val mode: PlacementMode,
) {
  private val tiles = mutableMapOf<AreaDefinition, SoloTile>()

  fun calculate(cards: List<CardDefinition>): List<Placement> {
    tiles.clear()
    require(cards.size == 4) { "exactly four cards are required" }
    cards.forEach {
      require(it.deck == PROJECT) { "${it.className} is not a project card" }
      require(mode != PlacementMode.COMPATIBILITY || it.cost > 0) {
        "${it.className} has cost 0, which compatibility mode rejects"
      }
    }

    val firstCity = placeCity(cards[0], availableCityAreas())
    val secondCity = placeCity(cards[1], availableCityAreas().asReversed())

    val secondGreeneryCandidates = availableNeighbors(secondCity)
    val greenCardsAndAreas =
        if (
            secondGreeneryCandidates.size == 1 &&
                secondGreeneryCandidates.single() in clockwiseNeighbors(firstCity)
        ) {
          val secondGreenery = placeGreenery(cards[3], secondCity)
          val firstGreenery = placeGreenery(cards[2], firstCity)
          firstGreenery to secondGreenery
        } else {
          val firstGreenery = placeGreenery(cards[2], firstCity)
          val secondGreenery = placeGreenery(cards[3], secondCity)
          firstGreenery to secondGreenery
        }

    return listOf(
        Placement(SoloTile.CITY, 1, firstCity, cards[0]),
        Placement(SoloTile.GREENERY, 1, greenCardsAndAreas.first, cards[2]),
        Placement(SoloTile.CITY, 2, secondCity, cards[1]),
        Placement(SoloTile.GREENERY, 2, greenCardsAndAreas.second, cards[3]),
    )
  }

  private fun placeCity(card: CardDefinition, candidates: List<AreaDefinition>): AreaDefinition {
    val index = index(card.cost)
    require(index < candidates.size) {
      "${card.className} costs ${card.cost}, but only ${candidates.size} legal city areas remain"
    }
    return candidates[index].also { tiles[it] = SoloTile.CITY }
  }

  private fun placeGreenery(card: CardDefinition, city: AreaDefinition): AreaDefinition {
    val candidates = availableNeighbors(city)
    require(candidates.isNotEmpty()) { "no legal greenery area remains next to ${city.className}" }
    return candidates[index(card.cost) % candidates.size].also { tiles[it] = SoloTile.GREENERY }
  }

  private fun index(cost: Int): Int =
      when (mode) {
        PlacementMode.COUNT_FROM_ZERO -> cost
        PlacementMode.COMPATIBILITY -> cost - 1
      }

  private fun availableCityAreas(): List<AreaDefinition> =
      map.areas
          .filter(::isAvailable)
          .filter { area -> clockwiseNeighbors(area).none { tiles[it] == SoloTile.CITY } }
          .sortedWith(compareBy(AreaDefinition::row, AreaDefinition::column))

  private fun availableNeighbors(area: AreaDefinition): List<AreaDefinition> =
      clockwiseNeighbors(area).filter(::isAvailable)

  private fun isAvailable(area: AreaDefinition): Boolean =
      area.kind !in RESERVED_AREA_KINDS && area !in tiles

  private fun clockwiseNeighbors(area: AreaDefinition): List<AreaDefinition> {
    val neighbors = map.areas.hexNeighbors(area.row, area.column)
    val topLeft = map.areas[area.row - 1, area.column - 1] ?: return neighbors
    check(neighbors.last() == topLeft)
    return listOf(topLeft) + neighbors.dropLast(1)
  }

  private companion object {
    val RESERVED_AREA_KINDS: Set<ClassName> = setOf(cn("WaterArea"), cn("NoctisArea"))
  }
}

internal fun calculateSoloPlacements(arguments: List<String>): List<Placement> {
  val compatibility = arguments.firstOrNull() == "--compatibility"
  val namesInOrder = if (compatibility) arguments.drop(1) else arguments
  require(namesInOrder.size == 5) {
    "usage: solo-placement [--compatibility] MAP CITY1_CARD CITY2_CARD " +
        "GREENERY1_CARD GREENERY2_CARD"
  }
  val names = namesInOrder.map(::cn)
  val map = Canon.marsMap(names.first())
  val cards = names.drop(1).map(Canon::card)
  val mode = if (compatibility) PlacementMode.COMPATIBILITY else PlacementMode.COUNT_FROM_ZERO
  return SoloPlacementCalculator(map, mode).calculate(cards)
}

internal fun formatPlacements(placements: List<Placement>): String {
  val drawOrder = listOf(placements[0], placements[2], placements[1], placements[3])
  return buildString {
    appendLine("Cards (draw order):")
    drawOrder.forEachIndexed { index, placement ->
      appendLine("${index + 1}. ${placement.card.className}: ${placement.card.cost}")
    }
    appendLine("Placements:")
    placements.forEach { placement ->
      val label = placement.tile.name.lowercase().replaceFirstChar(Char::uppercase)
      appendLine(
          "$label ${placement.ordinal}: ${placement.area.className} " +
              "(${placement.card.className}, ${placement.card.cost})"
      )
    }
  }
      .trimEnd()
}

public fun main(args: Array<String>) {
  try {
    println(formatPlacements(calculateSoloPlacements(args.toList())))
  } catch (e: IllegalArgumentException) {
    System.err.println(e.message)
    exitProcess(2)
  }
}
