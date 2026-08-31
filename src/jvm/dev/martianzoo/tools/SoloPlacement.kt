package dev.martianzoo.tools

import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Class
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.MarsMapDefinition
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.cardCost
import kotlin.system.exitProcess

internal enum class SoloTile {
  CITY,
  GREENERY,
}

private enum class PlacementMode {
  STANDARD,
  COMPATIBILITY,
}

internal data class Placement(
    val tile: SoloTile,
    val ordinal: Int,
    val area: AreaDefinition,
    val card: Class,
    val drawOrdinal: Int,
)

private class SoloPlacementCalculator(
    private val map: MarsMapDefinition,
    private val mode: PlacementMode,
) {
  private val tiles = mutableMapOf<AreaDefinition, SoloTile>()

  fun calculate(cards: List<Class>): List<Placement> {
    tiles.clear()
    require(cards.size == 4) { "exactly four cards are required" }
    cards.forEach {
      require(cardBack(it)?.className == cn("ProjectCard")) {
        "${soloPlacementVocabulary.displayName(it.className)} is not a project card"
      }
      require(mode != PlacementMode.COMPATIBILITY || cardCost(it) > 0) {
        "${soloPlacementVocabulary.displayName(it.className)} has cost 0, which compatibility mode rejects"
      }
    }

    return when (mode) {
      PlacementMode.STANDARD -> calculateOfficialOrder(cards)
      PlacementMode.COMPATIBILITY -> calculateCompatibilityOrder(cards)
    }
  }

  private fun calculateOfficialOrder(cards: List<Class>): List<Placement> {
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
        Placement(SoloTile.CITY, 1, firstCity, cards[0], 1),
        Placement(SoloTile.GREENERY, 1, greenCardsAndAreas.first, cards[2], 3),
        Placement(SoloTile.CITY, 2, secondCity, cards[1], 2),
        Placement(SoloTile.GREENERY, 2, greenCardsAndAreas.second, cards[3], 4),
    )
  }

  private fun calculateCompatibilityOrder(cards: List<Class>): List<Placement> {
    val firstCity = placeCity(cards[0], availableCityAreas())
    val firstGreenery = placeGreenery(cards[1], firstCity)
    val secondCity = placeCity(cards[2], availableCityAreas().asReversed())
    val secondGreenery = placeGreenery(cards[3], secondCity)
    return listOf(
        Placement(SoloTile.CITY, 1, firstCity, cards[0], 1),
        Placement(SoloTile.GREENERY, 1, firstGreenery, cards[1], 2),
        Placement(SoloTile.CITY, 2, secondCity, cards[2], 3),
        Placement(SoloTile.GREENERY, 2, secondGreenery, cards[3], 4),
    )
  }

  private fun placeCity(card: Class, candidates: List<AreaDefinition>): AreaDefinition {
    val index = index(cardCost(card))
    require(index < candidates.size) {
      "${soloPlacementVocabulary.displayName(card.className)} costs ${cardCost(card)}, " +
          "but only ${candidates.size} legal city areas remain"
    }
    return candidates[index].also { tiles[it] = SoloTile.CITY }
  }

  private fun placeGreenery(card: Class, city: AreaDefinition): AreaDefinition {
    val candidates = availableNeighbors(city)
    require(candidates.isNotEmpty()) { "no legal greenery area remains next to ${city.className}" }
    return candidates[index(cardCost(card)) % candidates.size].also {
      tiles[it] = SoloTile.GREENERY
    }
  }

  private fun index(cost: Int): Int =
      when (mode) {
        PlacementMode.STANDARD -> cost
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
    "usage: solo-placement [--compatibility] MAP CARD1 CARD2 CARD3 CARD4"
  }
  val names = namesInOrder.map(::cn).map(soloPlacementVocabulary::canonicalName)
  val requestedMap = names.first()
  val mapName =
      cn("${requestedMap}Map").takeIf { it in soloPlacementCatalog.allClassNames } ?: requestedMap
  val map = soloPlacementCatalog.marsMap(mapName)
  val cards = names.drop(1).map(soloPlacementCatalog::card)
  val mode = if (compatibility) PlacementMode.COMPATIBILITY else PlacementMode.STANDARD
  return SoloPlacementCalculator(map, mode).calculate(cards)
}

internal fun formatPlacements(placements: List<Placement>): String {
  val drawOrder = placements.sortedBy(Placement::drawOrdinal)
  return buildString {
    appendLine("Cards (draw order):")
    drawOrder.forEachIndexed { index, placement ->
      appendLine(
          "${index + 1}. ${soloPlacementVocabulary.displayName(placement.card.className)}: " +
              cardCost(placement.card)
      )
    }
    appendLine("Placements:")
    placements.forEach { placement ->
      val label = placement.tile.name.lowercase().replaceFirstChar(Char::uppercase)
      appendLine(
          "$label ${placement.ordinal}: ${placement.area.className} " +
              "(${soloPlacementVocabulary.displayName(placement.card.className)}, ${cardCost(placement.card)})"
      )
    }
  }
      .trimEnd()
}

private val soloPlacementCatalog: TfmCatalog = Canon

private val soloPlacementVocabulary: Vocabulary = run {
  val excludedClasses =
      soloPlacementCatalog.bundles
          .flatMap { bundle -> bundle.moduleClassExclusions.values.flatten() }
          .toSet()
  Vocabulary.create(
      soloPlacementCatalog,
      activeClassNames = soloPlacementCatalog.allClassNames - excludedClasses,
  )
}

public fun main(args: Array<String>) {
  try {
    println(formatPlacements(calculateSoloPlacements(args.toList())))
  } catch (e: IllegalArgumentException) {
    System.err.println(e.message)
    exitProcess(2)
  }
}
