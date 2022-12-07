package dev.martianzoo.tfm.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.martianzoo.util.Grid
import java.util.*
import kotlin.text.RegexOption.DOT_MATCHES_ALL

internal object MoshiReader {
  private val MOSHI = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  // Components

  internal data class ComponentTypeList(val components: List<ComponentDefinition>) {
    fun toMap() = components.associateBy { it.name }.also { require(it.size == components.size) }
  }

  private val MOSHI_COMPONENT = MOSHI.adapter(ComponentTypeList::class.java).nullSafe().lenient()

  internal fun readComponentTypes(json5: String) = MOSHI_COMPONENT.fromJson(json5ToJson(json5))!!.toMap()

  // Cards

  internal data class CardList(val cards: List<CardDefinition>) {
    fun toMap() = cards.associateBy { it.id }.also { require(it.size == cards.size) }
  }

  private val MOSHI_CARD = MOSHI.adapter(CardList::class.java).nullSafe().lenient()

  internal fun readCards(json5: String) = MOSHI_CARD.fromJson(json5ToJson(json5))!!.toMap()

  // Maps

  internal data class MapsImportFormat(val maps: List<MapImportFormat>, val legend: Map<Char, String>)
  internal data class MapImportFormat(val id: String, val rows: List<String>)

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

  private val MOSHI_MAP = MOSHI.adapter(MapsImportFormat::class.java).nullSafe().lenient()

  // Nothing like three different meanings of the same word in the same place
  fun readMaps(json5: String): Map<String, Grid<MarsAreaDefinition>> {
    val import: MapsImportFormat = MOSHI_MAP.fromJson(json5ToJson(json5))!!
    val legend = Legend(import.legend)

    return import.maps.associateBy(MapImportFormat::id) { map ->
      val areas = map.rows.flatMapIndexed { row, line ->
        line.chunked(6)
            .map(String::trim)
            .withIndex()
            .filter { it.value.isNotEmpty() }
            .map { (column, code) ->
              val (type, bonus) = legend.translate(code)
              MarsAreaDefinition(map.id, row + 1, column, type, bonus, code)
            }
      }
      Grid.grid(areas, { it.row }, { it.column })
    }
  }

  // Stuff

  private fun json5ToJson(json5: String): String {
    return TRAILING_COMMA_REGEX.replace(json5, "")
  }

  private val TRAILING_COMMA_REGEX = Regex(""",(?=\s*(//[^\n]*\n\s*)?[\]}])""", DOT_MATCHES_ALL)
}
