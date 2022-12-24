package dev.martianzoo.tfm.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.PetsParser.Components.oneLineComponentDef
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.util.Grid
import dev.martianzoo.util.associateByStrict
import java.util.*
import kotlin.text.RegexOption.DOT_MATCHES_ALL

internal object JsonReader {
  // Cards
  internal fun readCards(json5: String) = MOSHI_CARD.fromJson(json5ToJson(json5))!!.toMap()

  internal data class CardList(val cards: List<CardDefinition>) {
    fun toMap() = cards.associateByStrict { it.id }
  }

  // Maps

  internal data class MapsImportFormat(val maps: List<MapImportFormat>, val legend: Map<Char, String>)
  internal data class MapImportFormat(val name: String, val rows: List<String>)
  internal data class Legend(private val table: Map<Char, String>) {
    fun translate(code: String): Pair<String, String?> {
      val q: Queue<Char> = ArrayDeque(code.trim().toList())
      val result = mutableListOf<String>()
      val type = table[q.poll()]!!
      while (q.any()) {
        val next = q.remove()
        result.add(
            when (next) {
              in '2'..'9' -> "$next ${table[q.remove()]!!}"
              else -> if (q.peek() == next) {
                q.poll()
                "2 ${table[next]!!}"
              } else {
                table[next]!!
              }
            },
        )
      }
      return type to emptyToNull(result.joinToString())
    }

    private fun emptyToNull(s: String?) = if ((s?.length ?: 0) > 0) s else null
  }

  // Milestones
  internal fun readMilestones(json5: String) = MOSHI_MILESTONE.fromJson(json5ToJson(json5))!!.toMap()

  internal data class MilestoneList(val milestones: List<MilestoneDefinition>) {
    fun toMap() = milestones.associateBy { it.id }.also { require(it.size == milestones.size) }
  }

  // Nothing like three different meanings of the same word in the same place
  fun readMaps(json5: String): Map<String, Grid<MarsAreaDefinition>> {
    val import: MapsImportFormat = MOSHI_MAP.fromJson(json5ToJson(json5))!!
    val legend = Legend(import.legend)

    return import.maps.associateBy(MapImportFormat::name) { map ->
      val areas = map.rows.flatMapIndexed { row, line ->
        line.chunked(6)
            .map(String::trim)
            .withIndex()
            .filter { it.value.isNotEmpty() }
            .map { (column, code) ->
              val (type, bonus) = legend.translate(code)
              MarsAreaDefinition(map.name, row + 1, column, type, bonus)
            }
      }
      Grid.grid(areas, { it.row }, { it.column })
    }
  }

  fun auxiliaryComponentDefinitions(cardDefs: Collection<CardDefinition>): Map<String, ComponentDef> =
    cardDefs.flatMap { it.extraComponentsText }
        .map { parse(oneLineComponentDef, it) }
        .associateBy { it.name }

  // Stuff

  private fun json5ToJson(json5: String): String {
    return TRAILING_COMMA_REGEX.replace(json5, "")
  }

  private val TRAILING_COMMA_REGEX = Regex(""",(?=\s*(//[^\n]*\n\s*)?[\]}])""", DOT_MATCHES_ALL)

  private val MOSHI = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val MOSHI_CARD = MOSHI.adapter(CardList::class.java).nullSafe().lenient()
  private val MOSHI_MILESTONE = MOSHI.adapter(MilestoneList::class.java).nullSafe().lenient()
  private val MOSHI_MAP = MOSHI.adapter(MapsImportFormat::class.java).nullSafe().lenient()
}
