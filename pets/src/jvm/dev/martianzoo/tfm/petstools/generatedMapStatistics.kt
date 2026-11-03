package dev.martianzoo.tfm.petstools

import dev.martianzoo.tfm.canon.MarsMapDefinition
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition
import kotlin.math.abs

private val bonusNames =
    linkedMapOf(
        'C' to "Card",
        'P' to "Plant",
        'S' to "Steel",
        'T' to "Titanium",
        'E' to "Energy",
        'H' to "Heat",
        'D' to "Delegate",
        'R' to "Standard resource",
        'F' to "Temperature step",
        'O' to "Ocean",
        '4' to "4 MC cost",
        '6' to "6 MC cost",
        'X' to "Cimmeria special",
        'V' to "Venus special",
    )

private fun groups(
    map: MarsMapDefinition,
    include: (AreaDefinition) -> Boolean,
): List<Set<AreaDefinition>> {
  val remaining = map.areas.filter(include).toMutableSet()
  val groups = mutableListOf<Set<AreaDefinition>>()
  while (remaining.isNotEmpty()) {
    val first = remaining.first()
    remaining.remove(first)
    val pending = mutableListOf(first)
    val group = mutableSetOf<AreaDefinition>()
    while (pending.isNotEmpty()) {
      val area = pending.removeLast()
      group.add(area)
      map.areas.hexNeighbors(area.row, area.column).forEach {
        if (remaining.remove(it)) pending.add(it)
      }
    }
    groups.add(group)
  }
  return groups
}

private fun groupSummary(groups: List<Set<AreaDefinition>>): String =
    groups
        .map { it.size }
        .groupingBy { it }
        .eachCount()
        .toSortedMap(reverseOrder())
        .entries
        .joinToString("; ") { "${it.value}x${it.key}" }

// A narrow land space has water on both sides in at least one of the three hex directions.
private fun narrowLandCount(map: MarsMapDefinition): Int =
    map.areas.count { area ->
      area.kind.toString() != "WaterArea" &&
          listOf(1 to 0, 0 to 1, 1 to 1).any { (dr, dc) ->
            map.areas[area.row + dr, area.column + dc]?.kind?.toString() == "WaterArea" &&
                map.areas[area.row - dr, area.column - dc]?.kind?.toString() == "WaterArea"
          }
    }

private fun iconCount(area: AreaDefinition): Int =
    area.expandedBonusCodes.sumOf { if (it == 'X' || it == 'V') 2 else 1 }

private fun plantEdges(map: MarsMapDefinition): Int =
    map.areas.sumOf { area ->
      if ('P' !in area.expandedBonusCodes) 0
      else map.areas.hexNeighbors(area.row, area.column).count { 'P' in it.expandedBonusCodes }
    } / 2

private fun plantGroups(map: MarsMapDefinition): List<Set<AreaDefinition>> =
    groups(map) { 'P' in it.expandedBonusCodes }

private fun plantIcons(map: MarsMapDefinition): Int =
    map.areas.sumOf { area -> area.expandedBonusCodes.count { it == 'P' } }

private fun repeatedBonusAreas(areas: Collection<AreaDefinition>): Int = areas.count { area ->
  area.expandedBonusCodes.groupingBy { it }.eachCount().values.any { it > 1 }
}

private fun bestThreeRows(map: MarsMapDefinition): Int =
    (1..7).maxOf { row ->
      map.areas
          .filter { it.row in row..row + 2 }
          .sumOf { area -> area.expandedBonusCodes.count { it == 'P' } }
    }

private fun localBonusKinds(map: MarsMapDefinition, area: AreaDefinition): Int =
    (map.areas.hexNeighbors(area.row, area.column) + area)
        .flatMap { it.expandedBonusCodes }
        .filter { it in "PCSTEHDRFOXV" }
        .toSet()
        .size

internal fun maxNearbyIcons(map: MarsMapDefinition, symbol: Char): Int =
    map.areas.maxOf { center ->
      map.areas.sumOf { area ->
        val dr = area.row - center.row
        val dc = area.column - center.column
        if (maxOf(abs(dr), abs(dc), abs(dr - dc)) <= 2)
            area.expandedBonusCodes.count { it == symbol }
        else 0
      }
    }

private fun maxNearbyAllIcons(map: MarsMapDefinition): Int =
    map.areas.maxOf { center ->
      map.areas.sumOf { area ->
        val dr = area.row - center.row
        val dc = area.column - center.column
        if (maxOf(abs(dr), abs(dc), abs(dr - dc)) <= 2) iconCount(area) else 0
      }
    }

private fun edgeDistance(area: AreaDefinition): Int =
    4 - maxOf(abs(area.row - 5), abs(area.column - 5), abs(area.row - area.column))

// Screening is optional and compares independent generated maps with broad observed reference
// ranges.
internal fun mapOutliers(map: MarsMapDefinition): List<String> = buildList {
  val water = groups(map) { it.kind.toString() == "WaterArea" }
  val waterEdges = water.sumOf { group ->
    group.sumOf { map.areas.hexNeighbors(it.row, it.column).count { n -> n in group } } / 2
  }
  val plants = plantGroups(map)
  val icons = plantIcons(map)
  val totalIcons = map.areas.sumOf(::iconCount)
  val emptySpaces = map.areas.count { iconCount(it) == 0 }
  val volcanos = map.areas.filter { it.kind.toString() == "VolcanicArea" }
  val neighborhoodVariety = map.areas.map { localBonusKinds(map, it) }
  if (neighborhoodVariety.any { it >= 5 } || neighborhoodVariety.count { it >= 4 } > 17)
      add("too many bonus kinds in neighborhoods")
  if (
      volcanos.isNotEmpty() &&
          (volcanos.count { edgeDistance(it) == 0 } < 3 ||
              volcanos.any { edgeDistance(it) > 1 } ||
              volcanos.count { area -> area.expandedBonusCodes.any { it in "CST" } } < 3)
  )
      add("unusual volcanic areas")
  if (repeatedBonusAreas(volcanos) > 2) add("too many volcanic spaces with repeated bonuses")
  if (maxNearbyIcons(map, 'S') > 11 || maxNearbyIcons(map, 'T') > 4)
      add("too much metal in a small region")
  if (
      maxNearbyIcons(map, 'C') > 6 ||
          maxNearbyIcons(map, 'P') > 21 ||
          maxNearbyAllIcons(map) !in 21..30
  )
      add("unusual local bonus density")
  if (totalIcons !in 58..70) add("unusual total bonus count")
  val cardIcons = map.areas.sumOf { area -> area.expandedBonusCodes.count { it == 'C' } }
  val titaniumIcons = map.areas.sumOf { area -> area.expandedBonusCodes.count { it == 'T' } }
  if (cardIcons > 10 || titaniumIcons > 9) add("unusual card or titanium total")
  if (emptySpaces !in 13..29) add("unusual count of empty spaces")
  val plantWaterIcons =
      map.areas
          .filter { it.kind.toString() == "WaterArea" }
          .sumOf { it.expandedBonusCodes.count { code -> code == 'P' } }
  val steelWaterIcons =
      map.areas
          .filter { it.kind.toString() == "WaterArea" }
          .sumOf { it.expandedBonusCodes.count { code -> code == 'S' } }
  val titaniumWaterIcons =
      map.areas
          .filter { it.kind.toString() == "WaterArea" }
          .sumOf { it.expandedBonusCodes.count { code -> code == 'T' } }
  val steelIcons = map.areas.sumOf { it.expandedBonusCodes.count { code -> code == 'S' } }
  if (plantWaterIcons * 6 < icons) add("too few plants on water")
  if (steelWaterIcons * 4 > steelIcons) add("too much steel on water")
  if (titaniumWaterIcons * 5 > titaniumIcons * 3) add("too much titanium on water")
  if (narrowLandCount(map) > 3) add("too many narrow land spaces")
  if (water.size !in 2..5 || waterEdges !in 7..16 || water.any { it.size > 10 })
      add("unusual water grouping")
  if (icons !in 26..38 || plants.sumOf { it.size } !in 19..26) add("unusual plant count")
  if (
      (plants.maxOfOrNull { it.size } ?: 0) < 12 ||
          plantEdges(map) < 24 ||
          bestThreeRows(map).toDouble() / icons < 0.55
  )
      add("plants too scattered")
  if (map.areas.any { iconCount(it) > 3 } || map.areas.count { iconCount(it) == 3 } > 3)
      add("too many icons on spaces")
}

// Zero is the average distance of all spaces. Each side is scaled to its own extreme.
private fun attraction(
    areas: Collection<AreaDefinition>,
    weight: (AreaDefinition) -> Int,
    distance: (AreaDefinition) -> Int,
): Double {
  val baseline = areas.map(distance).average()
  val weighted =
      areas.sumOf { area -> distance(area) * weight(area) }.toDouble() / areas.sumOf(weight)
  val difference = baseline - weighted
  val scale = if (difference >= 0) baseline else areas.maxOf(distance) - baseline
  return if (scale == 0.0) 0.0 else difference / scale
}

internal fun printGeneratedMapStatistics(map: MarsMapDefinition, name: String) {
  val areas = map.areas
  println("\n$name")
  val waterEdges =
      areas.sumOf { area ->
        if (area.kind.toString() != "WaterArea") 0
        else
            map.areas.hexNeighbors(area.row, area.column).count {
              it.kind.toString() == "WaterArea"
            }
      } / 2
  val neighborhoodVariety = areas.map { localBonusKinds(map, it) }
  val volcanos = areas.filter { it.kind.toString() == "VolcanicArea" }
  println(
      "Local bonus kinds: max ${neighborhoodVariety.max()}; neighborhoods with 4+: ${neighborhoodVariety.count { it >= 4 }}; with 5+: ${neighborhoodVariety.count { it >= 5 }}"
  )
  if (volcanos.isNotEmpty())
      println(
          "Volcanic spaces: ${volcanos.count { edgeDistance(it) == 0 }} on outer edge; ${volcanos.count { a -> a.expandedBonusCodes.any { it in "CST" } }} with cards or metal; ${repeatedBonusAreas(volcanos)} with repeated bonuses"
      )
  println("Water-to-water shared edges: $waterEdges")
  println("Narrow land spaces (water on opposite sides): ${narrowLandCount(map)}")
  val plants = plantGroups(map)
  println(
      "Plant groups: ${groupSummary(plants)}; shared edges: ${plantEdges(map)}; best 3 rows: ${bestThreeRows(map)}/${plantIcons(map)} icons"
  )
  println("Spaces with 3 or more bonus icons: ${areas.count { iconCount(it) >= 3 }}")
  println(
      "Total bonus icons: ${areas.sumOf(::iconCount)}; empty spaces: ${areas.count { iconCount(it) == 0 }}"
  )
  println(
      "Most icons within 2 hex steps: plant ${maxNearbyIcons(map, 'P')}; card ${maxNearbyIcons(map, 'C')}; steel ${maxNearbyIcons(map, 'S')}; titanium ${maxNearbyIcons(map, 'T')}; all ${maxNearbyAllIcons(map)}"
  )
  for (kind in listOf("WaterArea", "VolcanicArea", "NoctisArea")) {
    val clumps = groups(map) { it.kind.toString() == kind }
    if (clumps.isNotEmpty()) {
      val summary = groupSummary(clumps)
      val weight: (AreaDefinition) -> Int = { if (it.kind.toString() == kind) 1 else 0 }
      val equator = attraction(areas, weight) { abs(it.row - 5) }
      val center =
          attraction(areas, weight) {
            maxOf(abs(it.row - 5), abs(it.column - 5), abs(it.row - it.column))
          }
      println(
          "$kind clumps: $summary (equator %+.2f; center %+.2f)"
              .format(java.util.Locale.ROOT, equator, center)
      )
      if (kind == "WaterArea") {
        val details =
            clumps
                .sortedByDescending { it.size }
                .joinToString("; ") { group ->
                  val edges =
                      group.sumOf { area ->
                        map.areas.hexNeighbors(area.row, area.column).count { it in group }
                      } / 2
                  "${group.size} spaces: $edges edges"
                }
        println("Water clump edges: $details")
      }
    }
  }
  for ((symbol, label) in bonusNames) {
    val counts =
        areas.map { it.expandedBonusCodes.count { code -> code == symbol } }.filter { it > 0 }
    if (counts.isEmpty()) continue
    val distribution =
        counts
            .groupingBy { it }
            .eachCount()
            .toSortedMap()
            .entries
            .joinToString("; ") { "${it.value}x${it.key}" }
    val weight: (AreaDefinition) -> Int = {
      it.expandedBonusCodes.count { code -> code == symbol }
    }
    val equator = attraction(areas, weight) { abs(it.row - 5) }
    val center =
        attraction(areas, weight) {
          maxOf(abs(it.row - 5), abs(it.column - 5), abs(it.row - it.column))
        }
    val water = attraction(areas, weight) { if (it.kind.toString() == "WaterArea") 0 else 1 }
    println(
        "$label bonuses: $distribution (equator %+.2f; center %+.2f; water %+.2f)"
            .format(java.util.Locale.ROOT, equator, center, water)
    )
  }
  areas
      .filter {
        'X' in it.expandedBonusCodes ||
            'V' in it.expandedBonusCodes ||
            'O' in it.expandedBonusCodes ||
            'F' in it.expandedBonusCodes
      }
      .forEach {
        val description =
            when {
              'X' in it.expandedBonusCodes -> "Colony for 5 MC if Colonies is included"
              'V' in it.expandedBonusCodes -> "Venus step for 5 MC if Venus Next is included"
              'O' in it.expandedBonusCodes -> "Ocean for 6 MC"
              else -> "Temperature step for 4 MC"
            }
        println("Special area ${it.className}: $description")
      }
}
