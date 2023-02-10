package dev.martianzoo.tfm.data

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
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

  fun readActions(json5: String): List<StandardActionDefinition> {
    val import = fromJson5<ActionsImport>(json5)

    return import.actions.map { it.complete(false) } + import.projects.map { it.complete(true) }
  }

  private class ActionsImport(
      val actions: List<IncompleteActionDef>,
      val projects: List<IncompleteActionDef>
  ) {

    class IncompleteActionDef(val id: ClassName, val bundle: String, val action: String) {
      fun complete(project: Boolean) = StandardActionDefinition(id, bundle, project, action)
    }
  }

  // MAPS

  fun readMaps(json5: String): List<MarsMapDefinition> = fromJson5<MapsImport>(json5).definitions

  private class MapsImport(val maps: List<MapImport>, val legend: Map<Char, String>) {
    val definitions: List<MarsMapDefinition> by lazy {
      val leg = Legend(legend)
      maps.map { it.toDefinition(leg) }
    }

    class MapImport(
        val name: ClassName,
        val bundle: String,
        val rows: List<List<String>>,
    ) {
      internal fun toDefinition(legend: Legend): MarsMapDefinition {
        fun mapArea(row0Index: Int, col0Index: Int, code: String, legend: Legend): AreaDefinition? {
          if (code.isEmpty()) return null
          return AreaDefinition(
              name, bundle,
              row0Index + 1, col0Index + 1,
              legend.getType(code), legend.getBonus(code),
              code)
        }

        val areas =
            rows.flatMapIndexed { row0Index, cells ->
              cells.mapIndexedNotNull { col0Index, code ->
                mapArea(row0Index, col0Index, code, legend)
              }
            }
        val grid = Grid.grid(areas, { it.row }, { it.column })
        return MarsMapDefinition(name, bundle, grid)
      }
    }

    class Legend(private val table: Map<Char, String>) {

      fun getType(code: String) = cn(lookUp(code[0]))
      fun getBonus(code: String): String? {
        val q = ArrayDeque(code.substring(1).toList())
        val result = generateSequence {
          @Suppress("KotlinConstantConditions") // a bogus warning
          if (q.any()) {
            when (val next = q.removeFirst()) {
              in '2'..'9' -> "$next ${lookUp(q.removeFirst())}"
              q.firstOrNull() -> "2 ${lookUp(q.removeFirst())}"
              else -> lookUp(next)
            }
          } else {
            null
          }
        }
        return result.joinToString().ifEmpty { null }
      }

      private fun lookUp(c: Char) = table[c] ?: "not found: $c"
    }
  }

  // HELPERS

  private inline fun <reified T : Any> fromJson5(input: String): T =
      Moshi.Builder()
          .add(ClassNameAdapter())
          .addLast(KotlinJsonAdapterFactory())
          .build()
          .adapter(T::class.java)
          .lenient()
          .fromJson(TRAILING_COMMA_REGEX.replace(input, ""))!!

  class ClassNameAdapter {
    @FromJson fun fromJson(card: String) = cn(card)
  }

  private val TRAILING_COMMA_REGEX = Regex(""",(?=\s*(//[^\n]*\n\s*)?[]}])""", DOT_MATCHES_ALL)
}
