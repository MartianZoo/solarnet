package dev.martianzoo.tfm.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.martianzoo.util.Grid
import kotlin.text.RegexOption.DOT_MATCHES_ALL

object JsonReader {

// CARDS

  fun readCards(json5: String) = fromJson5<CardList>(json5).cards

  private class CardList(val cards: List<CardDefinition>)

// MILESTONES

  fun readMilestones(json5: String) = fromJson5<MilestoneList>(json5).milestones

  private class MilestoneList(val milestones: List<MilestoneDefinition>)

// ACTIONS

  fun readActions(json5: String): List<ActionDefinition> {
    val import = fromJson5<ActionsImport>(json5)

    return import.actions.map { it.complete(false) } +
        import.projects.map { it.complete(true) }
  }

  private class ActionsImport(
      val actions: List<IncompleteActionDef>,
      val projects: List<IncompleteActionDef>) {

    class IncompleteActionDef(val id: String, val bundle: String, val action: String) {
      fun complete(project: Boolean) = ActionDefinition(id, bundle, project, action)
    }
  }

// MAPS

  fun readMaps(json5: String): Map<String, Grid<MapAreaDefinition>> =
      fromJson5<MapsImport>(json5).toGrids()

  private class MapsImport(val maps: List<MapImport>, val legend: Map<Char, String>) {
    fun toGrids() = maps.associateBy(MapImport::name) { it.toGrid(Legend(legend)) }

    internal class MapImport(val name: String, val bundle: String, val rows: List<List<String>>) {

      internal fun toGrid(legend: Legend): Grid<MapAreaDefinition> {
        val areas = rows.flatMapIndexed() { row0Index, cells ->
          cells.mapIndexedNotNull { col0Index, code ->
            mapArea(row0Index, col0Index, code, legend)
          }
        }
        return Grid.grid(areas, { it.row }, { it.column })
      }

      private fun mapArea(
          row0Index: Int, col0Index: Int, code: String, legend: Legend
      ): MapAreaDefinition? {
        if (code.isEmpty()) return null
        return MapAreaDefinition(
            name, bundle,
            row0Index + 1, col0Index + 1,
            legend.getType(code), legend.getBonus(code),
            code)
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

// HELPERS

  private inline fun <reified T : Any> fromJson5(input: String): T = Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
      .adapter(T::class.java)
      .lenient()
      .fromJson(TRAILING_COMMA_REGEX.replace(input, ""))!!

  private val TRAILING_COMMA_REGEX = Regex(""",(?=\s*(//[^\n]*\n\s*)?[\]}])""", DOT_MATCHES_ALL)
}
