package dev.martianzoo.tfm.petstools

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.util.Grid
import dev.martianzoo.tfm.canon.MarsMapDefinition
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.canon.MarsMapReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

// The generator has no map inputs: these choices are initial hypotheses to evaluate and refine.
internal fun generateMap(
    random: Random,
    withVolcanoes: Boolean = random.nextInt(3) != 0,
    withSpecial: Boolean = random.nextBoolean(),
    withNoctis: Boolean = random.nextInt(5) == 0,
): MarsMapDefinition {
  val coordinates =
      (-4..4).flatMap { r ->
        (maxOf(-4, r - 4)..minOf(4, r + 4)).map { c -> (r + 5) to (c + 5) }
      }
  val index = coordinates.withIndex().associate { it.value to it.index }
  fun neighbors(i: Int): List<Int> {
    val (r, c) = coordinates[i]
    return listOf(r - 1 to c, r to c + 1, r + 1 to c + 1, r + 1 to c, r to c - 1, r - 1 to c - 1)
        .mapNotNull(index::get)
  }
  fun weighted(candidates: List<Int>, weight: (Int) -> Double): Int {
    val weights = candidates.map(weight)
    var draw = random.nextDouble() * weights.sum()
    candidates.forEachIndexed { i, candidate ->
      draw -= weights[i]
      if (draw <= 0) return candidate
    }
    return candidates.last()
  }

  val kinds = MutableList(61) { 'L' }
  val bonuses = MutableList(61) { "" }
  repeat(12) {
    val candidates = kinds.indices.filter { kinds[it] == 'L' }
    val chosen = weighted(candidates) { i -> 1.0 + neighbors(i).count { kinds[it] == 'W' } * 4.0 }
    kinds[chosen] = 'W'
  }
  if (withVolcanoes) {
    val clusterSize = listOf(1, 1, 2, 3).random(random)
    repeat(4) { placed ->
      val candidates = kinds.indices.filter { kinds[it] == 'L' }
      val chosen =
          weighted(candidates) { i ->
            val (r, c) = coordinates[i]
            val distance = 4 - maxOf(abs(r - 5), abs(c - 5), abs(r - c))
            val edgeWeight =
                when (distance) {
                  0 -> 12.0
                  1 -> 1.0
                  else -> 0.02
                }
            val adjacent = neighbors(i).count { kinds[it] == 'V' }
            edgeWeight *
                if (placed in 1 until clusterSize) 0.08 + 15.0 * adjacent
                else if (adjacent == 0) 1.0 else 0.08
          }
      kinds[chosen] = 'V'
      val reward = listOf('S', 'S', 'T', 'C').random(random)
      bonuses[chosen] = reward.toString().repeat(if (random.nextDouble() < 0.3) 2 else 1)
    }
  }

  fun place(symbol: Char, count: Int, doubles: Double, region: Boolean = false) {
    val plantStyle = if (symbol == 'P') random.nextInt(4) else 0
    val centerRow =
        if (symbol == 'P' && plantStyle == 1) listOf(2, 8).random(random) else random.nextInt(2, 9)
    val centerCol = random.nextInt(2, 9)
    val existing = bonuses.count { symbol in it }
    repeat((count - existing).coerceAtLeast(0)) {
      val candidates =
          kinds.indices.filter { i ->
            'D' !in bonuses[i] &&
                symbol !in bonuses[i] &&
                (symbol != 'D' || bonuses[i].isEmpty()) &&
                bonuses[i].length <= 1
          }
      val chosen =
          weighted(candidates) { i ->
            val (r, c) = coordinates[i]
            val distance = abs(r - centerRow) + abs(c - centerCol)
            val regional =
                when {
                  symbol == 'P' -> {
                    val fromCenter =
                        if (plantStyle == 0) distance / 2.0 else abs(r - centerRow).toDouble()
                    val edgeDistance = 4 - maxOf(abs(r - 5), abs(c - 5), abs(r - c))
                    val edgePreference =
                        if (plantStyle == 1) 0.3 + 3.0 * exp(-edgeDistance.toDouble()) else 1.0
                    (0.15 + 3.0 * exp(-fromCenter / 1.5)) *
                        edgePreference *
                        (1.0 + 2.5 * neighbors(i).count { 'P' in bonuses[it] })
                  }
                  symbol == 'S' || symbol == 'T' -> {
                    val nearby =
                        coordinates.indices.sumOf { j ->
                          val (otherRow, otherCol) = coordinates[j]
                          val dr = r - otherRow
                          val dc = c - otherCol
                          if (maxOf(abs(dr), abs(dc), abs(dr - dc)) <= 2)
                              bonuses[j].count { it == symbol }
                          else 0
                        }
                    (0.6 + 2.2 * exp(-distance / 3.5)) * exp(-nearby * 0.35)
                  }
                  region -> 0.3 + 3.0 * exp(-distance / 2.5)
                  else -> 1.0
                }
            val waterPreference =
                if (kinds[i] != 'W') 1.0
                else
                    when (symbol) {
                      'P' -> 3.0
                      'S' -> 0.8
                      'T' -> 2.0
                      else -> 1.0
                    }
            regional * waterPreference * if (bonuses[i].isEmpty()) 1.0 else 0.35
          }
      val amount = if (random.nextDouble() < doubles) 2 else 1
      bonuses[chosen] += symbol.toString().repeat(amount)
    }
  }
  place('P', random.nextInt(19, 27), 0.45, true)
  place('C', random.nextInt(5, 8), 0.25)
  place('S', random.nextInt(6, 12), 0.40, true)
  place('T', random.nextInt(3, 7), 0.30, true)
  val extras =
      listOf('E', 'H', 'D', 'R').shuffled(random).take(listOf(0, 0, 1, 1, 1, 2).random(random))
  extras.forEach { place(it, random.nextInt(2, 5), if (it == 'D') 0.0 else 0.4, true) }
  if (withSpecial) {
    val candidates = kinds.indices.filter { kinds[it] == 'L' && bonuses[it].isEmpty() }
    bonuses[candidates.random(random)] = listOf("X", "V", "O6", "F4").random(random)
  }
  if (withNoctis) {
    val candidates =
        kinds.indices.filter { i ->
          kinds[i] == 'L' &&
              bonuses[i] !in listOf("X", "V", "O6", "F4") &&
              maxOf(
                  abs(coordinates[i].first - 5),
                  abs(coordinates[i].second - 5),
                  abs(coordinates[i].first - coordinates[i].second),
              ) <= 2
        }
    // Prefer land surrounded by many bonus icons, including its own placement bonus.
    fun richness(i: Int): Int =
        (neighbors(i) + i).sumOf { j ->
          when (bonuses[j]) {
            "X",
            "V",
            "O6",
            "F4" -> 2
            else -> bonuses[j].length
          }
        }
    val richest = candidates.sortedByDescending(::richness).take(8)
    kinds[richest.random(random)] = 'N'
  }
  val raw = coordinates.mapIndexed { i, (row, column) ->
    AreaDefinition(
        cn("Generated_${row}_$column"),
        row,
        column,
        cn("LandArea"),
        null,
        "${kinds[i]}${bonuses[i]}",
    )
  }
  val sketch = MarsMapDefinition(cn("GeneratedMap"), Grid.grid(raw, { it.row }, { it.column }))
  val source =
      "${MarsMapReader.GENERATED_AREAS_COMMENT}\n${renderDiagram(sketch)}\nCLASS Generated_1_1 : LandArea\n"
  return MarsMapReader.readMaps(source).single()
}

public fun main(args: Array<String>) {
  require(args.size == 6) {
    "Usage: generateMaps <seed> <count> <mars-image> <icons-directory> <output-directory> <screen>"
  }
  val seed = args[0].toLong()
  val count = args[1].toInt()
  require(count > 0)
  val random = Random(seed)
  val output = Path.of(args[4])
  Files.createDirectories(output)
  val screen = args[5].toBooleanStrict()
  val imageSuffix = if (screen) "" else "-raw"
  println("Seed: $seed; screening: $screen")
  var discarded = 0
  repeat(count) { i ->
    var discardedForMap = 0
    val withVolcanoes = random.nextInt(3) != 0
    val withSpecial = random.nextBoolean()
    val withNoctis = random.nextInt(5) == 0
    var map = generateMap(random, withVolcanoes, withSpecial, withNoctis)
    while (screen && mapOutliers(map).isNotEmpty()) {
      discarded++
      discardedForMap++
      check(discardedForMap < 10000) { "Could not find enough maps that pass screening" }
      map = generateMap(random, withVolcanoes, withSpecial, withNoctis)
    }
    println("\nGenerated map ${i + 1}:\n${renderDiagram(map)}")
    if (map.areas.any { 'V' in it.expandedBonusCodes }) {
      println("CLASS VenusPlacementBonus : Signal { This IF VenusNextExpansion: VenusStep, -5 MC }")
    }
    printGeneratedMapStatistics(map, "Generated map ${i + 1} statistics")
    val image = output.resolve("map-${seed}-${i + 1}$imageSuffix.png")
    renderMapPng(map, Path.of(args[2]), Path.of(args[3]), image)
    println("Image: $image")
  }
  if (screen) println("Candidates discarded: $discarded")
}
