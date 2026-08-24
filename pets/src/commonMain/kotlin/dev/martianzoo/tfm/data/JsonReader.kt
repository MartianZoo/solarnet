package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.CardDefinition.CardData
import dev.martianzoo.tfm.data.ColonyTileDefinition.ColonyTileData
import dev.martianzoo.tfm.data.MarsMapDefinition.AreaDefinition
import dev.martianzoo.util.Grid
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

public object JsonReader {

  /** Reads one bundle language file keyed by canonical class name. */
  public fun readDisplayNames(json5: String): Map<dev.martianzoo.pets.ast.ClassName, String> =
      fromJson5<Map<String, String>>(json5).mapKeys { (className) -> cn(className) }

  // CARDS

  public fun readCards(json5: String): List<CardData> = fromJson5<CardList>(json5).cards

  @Serializable private data class CardList(val cards: List<CardData>)

  // MAPS

  public fun readMaps(json5: String): List<MarsMapDefinition> =
      fromJson5<MapsImport>(json5).definitions()

  @Serializable
  private data class MapsImport(val maps: List<MapImport>, val legend: Map<String, String>) {
    fun definitions(): List<MarsMapDefinition> {
      val leg = Legend(legend.mapKeys { (key) -> key.toLegendKey() })
      return maps.map { it.toDefinition(leg) }
    }

    @Serializable
    data class MapImport(
        val name: String,
        val areaPrefix: String? = null,
        val defaultMilestones: String? = null,
        val defaultAwards: String? = null,
        val rows: List<List<String>>,
    ) {
      internal fun toDefinition(
          legend: Legend,
      ): MarsMapDefinition {
        val mapName = cn(name)
        val mapAreaPrefix = cn(areaPrefix ?: name)
        fun mapArea(
            row0Index: Int,
            col0Index: Int,
            code: String,
            legend: Legend,
        ): AreaDefinition? {
          val compactCode = code.filterNot { it.isWhitespace() }
          if (compactCode.isEmpty()) return null
          return AreaDefinition(
              mapAreaPrefix,
              row0Index + 1,
              col0Index + 1,
              legend.getType(compactCode),
              legend.getBonus(compactCode),
              compactCode,
          )
        }

        val areas = rows.flatMapIndexed { row0Index, cells ->
          cells.mapIndexedNotNull { col0Index, code ->
            mapArea(row0Index, col0Index, code, legend)
          }
        }
        val grid = Grid.grid(areas, { it.row }, { it.column })
        return MarsMapDefinition(
            mapName,
            grid,
            defaultMilestones?.let(::cn),
            defaultAwards?.let(::cn),
        )
      }
    }

    class Legend(private val table: Map<Char, String>) {

      fun getType(code: String) = cn(lookUp(code[0]))

      fun getBonus(code: String): String? {
        val q = ArrayDeque(code.substring(1).toList())
        val result = generateSequence {
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

  // COLONIES

  public fun readColonyTiles(json5: String): List<ColonyTileData> =
      fromJson5<ColonyTileList>(json5).colonyTiles

  @Serializable private data class ColonyTileList(val colonyTiles: List<ColonyTileData>)

  // HELPERS

  private inline fun <reified T : Any> fromJson5(input: String): T = JSON5.decodeFromString(input)

  private fun String.toLegendKey(): Char {
    require(length == 1) { "bad legend key: $this" }
    return single()
  }

  @OptIn(ExperimentalSerializationApi::class)
  private val JSON5 = Json {
    allowComments = true
    allowTrailingComma = true
    ignoreUnknownKeys = true
    isLenient = true
  }
}
