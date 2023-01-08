package dev.martianzoo.tfm.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.util.Grid
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import kotlin.text.RegexOption.DOT_MATCHES_ALL

internal object JsonReader {

// CARDS

  internal fun readCards(json5: String) = fromJson5<CardList>(json5).cards

  private class CardList(val cards: List<CardDefinition>)

// MILESTONES

  internal fun readMilestones(json5: String) = fromJson5<MilestoneList>(json5).milestones

  private class MilestoneList(val milestones: List<MilestoneDefinition>)

// MAPS

  fun readMaps(json5: String): Map<String, Grid<MapAreaDefinition>> =
      fromJson5<MapsImportFormat>(json5).toGrids()

  private class MapsImportFormat(val maps: List<MapImportFormat>, val legend: Map<Char, String>) {
    fun toGrids() = maps.associateBy(MapImportFormat::name) { it.toGrid(Legend(legend)) }

    internal class MapImportFormat(val name: String, val rows: List<List<String>>) {

      internal fun toGrid(legend: Legend): Grid<MapAreaDefinition> {
        val areas = rows.flatMapIndexed() { row0Index, cells ->
          cells.mapIndexedNotNull { col0Index, code ->
            mapArea(name, row0Index, col0Index, code, legend)
          }
        }
        return Grid.grid(areas, { it.row }, { it.column })
      }

      private fun mapArea(
          mapName: String, row0Index: Int, col0Index: Int, code: String, legend: Legend,
      ): MapAreaDefinition? {
        if (code.isEmpty()) return null
        return MapAreaDefinition(
            mapName, row0Index + 1, col0Index + 1, legend.getType(code), legend.getBonus(code), code
        )
      }
    }

    internal class Legend(private val table: Map<Char, String>) {

      fun getType(code: String) = lookUp(code[0])
      fun getBonus(code: String): String? {
        val q = ArrayDeque(code.substring(1).toList())
        val result = generateSequence {
          if (q.any()) {
            val next = q.removeFirst()
            when {
              next in '2'..'9' -> "$next ${lookUp(q.removeFirst())}"
              q.firstOrNull() == next -> "2 ${lookUp(q.removeFirst())}"
              else -> lookUp(next)
            }
          } else {
            null
          }
        }
        return emptyToNull(result.joinToString())
      }

      private fun emptyToNull(s: String) = if (s.isEmpty()) null else s
      private fun lookUp(c: Char) = table[c] ?: "not found: $c"
    }
  }

// HELP

  private inline fun <reified T : Any> fromJson5(input: String): T =
      Moshi.Builder()
          .addLast(KotlinJsonAdapterFactory())
          .build()
          .adapter(T::class.java)
          .lenient()
          .fromJson(TRAILING_COMMA_REGEX.replace(input, ""))!!

  private val TRAILING_COMMA_REGEX = Regex(""",(?=\s*(//[^\n]*\n\s*)?[\]}])""", DOT_MATCHES_ALL)
}
