package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition

/**
 * Calculates neutral solo setup on an empty [map] from four printed project-card costs. Returns
 * city 1, greenery 1, city 2, greenery 2. Water and Noctis areas are reserved.
 *
 * Standard mode uses zero-based costs: the first two cards choose cities in forward/reverse map
 * order, and the last two choose adjacent greenery clockwise from the upper-left hex, wrapping. A
 * sole greenery space shared by both cities is reserved for the second city's greenery.
 *
 * [compatibility] preserves the command-line tool's alternate procedure: costs are one-based and
 * cards choose city 1, greenery 1, city 2, greenery 2. This function neither draws cards nor
 * changes a World; callers consume the four cards and submit the resulting locations to their
 * normal placement tasks.
 *
 * @throws IllegalArgumentException for a count other than four, invalid costs, a city cost beyond
 *   the remaining legal areas, or a city without an available greenery space.
 */
public fun soloTileAreas(
    map: MarsMapDefinition,
    cardCosts: List<Int>,
    compatibility: Boolean = false,
): List<AreaDefinition> {
  require(cardCosts.size == 4) { "exactly four card costs are required" }
  require(cardCosts.all { it >= if (compatibility) 1 else 0 }) {
    "invalid solo placement card cost"
  }
  val cities = mutableSetOf<AreaDefinition>()
  val occupied = mutableSetOf<AreaDefinition>()
  val reserved = setOf(cn("WaterArea"), cn("NoctisArea"))

  fun index(cost: Int): Int = if (compatibility) cost - 1 else cost

  fun clockwiseNeighbors(area: AreaDefinition): List<AreaDefinition> {
    val neighbors = map.areas.hexNeighbors(area.row, area.column)
    val topLeft = map.areas[area.row - 1, area.column - 1] ?: return neighbors
    check(neighbors.last() == topLeft)
    return listOf(topLeft) + neighbors.dropLast(1)
  }

  fun isAvailable(area: AreaDefinition): Boolean = area.kind !in reserved && area !in occupied

  fun availableNeighbors(area: AreaDefinition): List<AreaDefinition> =
      clockwiseNeighbors(area).filter(::isAvailable)

  fun availableCityAreas(): List<AreaDefinition> =
      map.areas
          .filter(::isAvailable)
          .filter { area -> clockwiseNeighbors(area).none { it in cities } }
          .sortedWith(compareBy(AreaDefinition::row, AreaDefinition::column))

  fun placeCity(cost: Int, candidates: List<AreaDefinition>): AreaDefinition {
    val index = index(cost)
    require(index < candidates.size) {
      "Card costs $cost, " + "but only ${candidates.size} legal city areas remain"
    }
    return candidates[index].also {
      cities += it
      occupied += it
    }
  }

  fun placeGreenery(cost: Int, city: AreaDefinition): AreaDefinition {
    val candidates = availableNeighbors(city)
    require(candidates.isNotEmpty()) { "no legal greenery area remains next to ${city.className}" }
    return candidates[index(cost) % candidates.size].also {
      occupied += it
    }
  }

  fun standard(): List<AreaDefinition> {
    val firstCity = placeCity(cardCosts[0], availableCityAreas())
    val secondCity = placeCity(cardCosts[1], availableCityAreas().asReversed())

    val secondGreeneryCandidates = availableNeighbors(secondCity)
    val greeneries =
        if (
            secondGreeneryCandidates.size == 1 &&
                secondGreeneryCandidates.single() in clockwiseNeighbors(firstCity)
        ) {
          val secondGreenery = placeGreenery(cardCosts[3], secondCity)
          val firstGreenery = placeGreenery(cardCosts[2], firstCity)
          firstGreenery to secondGreenery
        } else {
          val firstGreenery = placeGreenery(cardCosts[2], firstCity)
          val secondGreenery = placeGreenery(cardCosts[3], secondCity)
          firstGreenery to secondGreenery
        }

    return listOf(firstCity, greeneries.first, secondCity, greeneries.second)
  }

  fun compatible(): List<AreaDefinition> {
    val firstCity = placeCity(cardCosts[0], availableCityAreas())
    val firstGreenery = placeGreenery(cardCosts[1], firstCity)
    val secondCity = placeCity(cardCosts[2], availableCityAreas().asReversed())
    val secondGreenery = placeGreenery(cardCosts[3], secondCity)
    return listOf(firstCity, firstGreenery, secondCity, secondGreenery)
  }

  return if (compatibility) compatible() else standard()
}
