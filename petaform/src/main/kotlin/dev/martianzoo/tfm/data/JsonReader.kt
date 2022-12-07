package dev.martianzoo.tfm.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.martianzoo.tfm.petaform.api.ComponentClassDeclaration
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import dev.martianzoo.util.Grid
import java.util.*
import kotlin.text.RegexOption.DOT_MATCHES_ALL

internal object JsonReader {
  // Cards
  internal fun readCards(json5: String) = MOSHI_CARD.fromJson(json5ToJson(json5))!!.toMap()

  internal data class CardList(val cards: List<CardDefinition>) {
    fun toMap() = cards.associateBy { it.id }.also { require(it.size == cards.size) }
  }

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

  fun auxiliaryComponentDefinitions(cardDefs: Collection<CardDefinition>): Map<String, ComponentDefinition> =
    cardDefs.flatMap { it.componentsPetaform }
        .map { parse<ComponentClassDeclaration>(it) }
        .map { ComponentDefinition(
            it.expression.className,
            it.abstract,
            it.supertypes.map(Any::toString).toSet(),
            it.expression.specializations.map(Any::toString),
            null,
            it.actions.map(Any::toString).toSet(),
            it.effects.map(Any::toString).toSet(),
          )
        }
        .associateBy { it.name }

  // You wouldn't normally use this, but have only a single map in play.
  fun combine(vararg defs: Collection<out Definition>): Map<String, ComponentDefinition> {
    val allDefns: List<Definition> = defs.flatMap { it }
    return allDefns.map { it.asComponentDefinition }.associateBy { it.name }.also {
      require(it.size == allDefns.size)
    }
  }

  // Stuff

  private fun json5ToJson(json5: String): String {
    return TRAILING_COMMA_REGEX.replace(json5, "")
  }

  private val TRAILING_COMMA_REGEX = Regex(""",(?=\s*(//[^\n]*\n\s*)?[\]}])""", DOT_MATCHES_ALL)

  private val MOSHI = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val MOSHI_CARD = MOSHI.adapter(CardList::class.java).nullSafe().lenient()
  private val MOSHI_MAP = MOSHI.adapter(MapsImportFormat::class.java).nullSafe().lenient()
}
