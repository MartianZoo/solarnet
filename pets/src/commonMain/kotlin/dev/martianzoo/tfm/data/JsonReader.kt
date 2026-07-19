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

  // CARDS

  fun readCards(json5: String): List<CardData> = fromJson5<CardList>(json5).cards

  fun readCards(json5: String, bundle: String): List<CardData> =
      readCards(json5).map {
        require(it.bundle == null) { "bundle must come from the containing directory" }
        it.copy(bundle = bundle)
      }

  @Serializable private data class CardList(val cards: List<CardData>)

  // MILESTONES

  fun readMilestones(json5: String): List<MilestoneDefinition> =
      fromJson5<MilestoneList>(json5).milestones.map { it.complete() }

  fun readMilestones(json5: String, bundle: String): List<MilestoneDefinition> =
      fromJson5<MilestoneList>(json5).milestones.map { it.complete(bundle) }

  @Serializable private data class MilestoneList(val milestones: List<MilestoneImport>)

  @Serializable
  private data class MilestoneImport(
      val id: String,
      val bundle: String? = null,
      val replaces: String? = null,
      val requirement: String,
  ) {
    fun complete(directoryBundle: String? = null): MilestoneDefinition {
      if (directoryBundle != null) {
        require(bundle == null) { "bundle must come from the containing directory" }
      }
      return MilestoneDefinition(
          id,
          directoryBundle ?: requireNotNull(bundle),
          replaces,
          requirement,
      )
    }
  }

  // ACTIONS

  fun readActions(json5: String): List<StandardActionDefinition> {
    return readActionsWithBundle(json5, null)
  }

  fun readActions(json5: String, bundle: String): List<StandardActionDefinition> {
    return readActionsWithBundle(json5, bundle)
  }

  private fun readActionsWithBundle(
      json5: String,
      directoryBundle: String?,
  ): List<StandardActionDefinition> {
    val import = fromJson5<ActionsImport>(json5)

    return import.actions.map { it.complete(false, directoryBundle) } +
        import.projects.map { it.complete(true, directoryBundle) }
  }

  @Serializable
  private data class ActionsImport(
      val actions: List<IncompleteActionDef>,
      val projects: List<IncompleteActionDef>,
  ) {

    @Serializable
    data class IncompleteActionDef(
        val id: String,
        val bundle: String? = null,
        val action: String? = null,
        val actions: List<String>? = null,
    ) {
      fun complete(project: Boolean, directoryBundle: String?): StandardActionDefinition {
        if (directoryBundle != null) {
          require(bundle == null) { "bundle must come from the containing directory" }
        }
        val realActions =
            if (action == null) {
              require(actions!!.any())
              actions
            } else {
              require(actions == null)
              listOf(action)
            }
        return StandardActionDefinition(
            cn(id),
            directoryBundle ?: requireNotNull(bundle),
            project,
            realActions,
        )
      }
    }
  }

  // MAPS

  fun readMaps(json5: String): List<MarsMapDefinition> = fromJson5<MapsImport>(json5).definitions

  fun readMaps(json5: String, bundle: String): List<MarsMapDefinition> =
      fromJson5<MapsImport>(json5).definitions(bundle)

  @Serializable
  private data class MapsImport(val maps: List<MapImport>, val legend: Map<String, String>) {
    val definitions: List<MarsMapDefinition> by lazy { definitions(null) }

    fun definitions(directoryBundle: String?): List<MarsMapDefinition> {
      val leg = Legend(legend.mapKeys { (key) -> key.toLegendKey() })
      return maps.map { it.toDefinition(leg, directoryBundle) }
    }

    @Serializable
    data class MapImport(
        val name: String,
        val bundle: String? = null,
        val rows: List<List<String>>,
    ) {
      internal fun toDefinition(legend: Legend, directoryBundle: String?): MarsMapDefinition {
        if (directoryBundle != null) {
          require(bundle == null) { "bundle must come from the containing directory" }
        }
        val owningBundle = directoryBundle ?: requireNotNull(bundle)
        val mapName = cn(name)
        fun mapArea(
            row0Index: Int,
            col0Index: Int,
            code: String,
            legend: Legend,
        ): AreaDefinition? {
          if (code.isEmpty()) return null
          return AreaDefinition(
              mapName,
              owningBundle,
              row0Index + 1,
              col0Index + 1,
              legend.getType(code),
              legend.getBonus(code),
              code,
          )
        }

        val areas = rows.flatMapIndexed { row0Index, cells ->
          cells.mapIndexedNotNull { col0Index, code ->
            mapArea(row0Index, col0Index, code, legend)
          }
        }
        val grid = Grid.grid(areas, { it.row }, { it.column })
        return MarsMapDefinition(mapName, owningBundle, grid)
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

  fun readColonyTiles(json5: String): List<ColonyTileData> =
      fromJson5<ColonyTileList>(json5).colonyTiles

  fun readColonyTiles(json5: String, bundle: String): List<ColonyTileData> =
      readColonyTiles(json5).map {
        require(it.bundle == null) { "bundle must come from the containing directory" }
        it.copy(bundle = bundle)
      }

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
