package dev.martianzoo.tfm.data

import dev.martianzoo.data.BundleMetadata
import dev.martianzoo.data.BundleMetadata.ConfigurationImplication
import dev.martianzoo.pets.Parsing.parse
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

  /**
   * Reads configuration implications and bootstrap validation alternatives stored with one bundle.
   */
  public fun readBundleMetadata(json5: String): BundleMetadata =
      fromJson5<BundleMetadataImport>(json5).complete()

  @Serializable
  private data class BundleMetadataImport(
      val configurationImplications: List<ConfigurationImplicationImport> = emptyList(),
      val moduleClassSelections: Map<String, List<ClassSelectionImport>> = emptyMap(),
      val bootstrapValidations: List<List<String>> = emptyList(),
  ) {
    fun complete(): BundleMetadata =
        BundleMetadata(
            configurationImplications.mapTo(
                linkedSetOf(),
                ConfigurationImplicationImport::complete,
            ),
            moduleClassSelections
                .map { (moduleName, selections) ->
                  cn(moduleName) to selections.mapTo(linkedSetOf(), ClassSelectionImport::complete)
                }
                .toMap(),
            bootstrapValidations.map { alternatives ->
              require(alternatives.isNotEmpty())
              alternatives.mapTo(linkedSetOf(), ::parse)
            },
        )
  }

  @Serializable
  private data class ClassSelectionImport(
      val className: String,
      val included: Boolean = true,
      val requirement: String? = null,
  ) {
    fun complete(): dev.martianzoo.data.ClassSelection =
        dev.martianzoo.data.ClassSelection(
            className = cn(className),
            included = included,
            requirement = requirement?.let(::parse),
        )
  }

  @Serializable
  private data class ConfigurationImplicationImport(
      val present: Set<String>,
      val absent: Set<String> = emptySet(),
      val include: Set<String>,
  ) {
    fun complete(): ConfigurationImplication =
        ConfigurationImplication(
            present.mapTo(linkedSetOf(), ::cn),
            absent.mapTo(linkedSetOf(), ::cn),
            include.mapTo(linkedSetOf(), ::cn),
        )
  }

  /** Reads one bundle language file keyed by canonical class name. */
  public fun readDisplayNames(json5: String): Map<dev.martianzoo.pets.ast.ClassName, String> =
      fromJson5<Map<String, String>>(json5).mapKeys { (className) -> cn(className) }

  // AWARDS

  public fun readAwards(json5: String): List<AwardDefinition> =
      fromJson5<AwardList>(json5).complete()

  @Serializable
  private data class AwardList(
      val setupRequirement: String? = null,
      val awards: List<AwardDefinition> = emptyList(),
      val groups: List<AwardList> = emptyList(),
  ) {
    init {
      require(awards.isNotEmpty() || groups.isNotEmpty())
      require(setupRequirement?.isNotBlank() != false)
    }

    fun complete(inheritedRequirement: String? = null): List<AwardDefinition> {
      val requirement = combineRequirements(inheritedRequirement, setupRequirement)
      return awards.map { definition ->
        requirement?.let(definition::withSetupRequirement) ?: definition
      } + groups.flatMap { it.complete(requirement) }
    }
  }

  // CARDS

  public fun readCards(json5: String): List<CardData> = fromJson5<CardList>(json5).cards

  @Serializable private data class CardList(val cards: List<CardData>)

  // MILESTONES

  public fun readMilestones(json5: String): List<MilestoneDefinition> =
      fromJson5<MilestoneList>(json5).definitions()

  @Serializable
  private data class MilestoneList(
      val milestones: List<MilestoneImport> = emptyList(),
      val setupRequirement: String? = null,
      val groups: List<MilestoneList> = emptyList(),
  ) {
    init {
      require(milestones.isNotEmpty() || groups.isNotEmpty())
      require(setupRequirement?.isNotBlank() != false)
    }

    fun definitions(inheritedRequirement: String? = null): List<MilestoneDefinition> {
      val requirement = combineRequirements(inheritedRequirement, setupRequirement)
      return milestones.map { it.complete(requirement) } +
          groups.flatMap { it.definitions(requirement) }
    }
  }

  @Serializable
  private data class MilestoneImport(
      val id: String,
      val replaces: String? = null,
      val requirement: String,
      val setupRequirement: String? = null,
  ) {
    init {
      require(setupRequirement?.isNotBlank() != false)
    }

    fun complete(groupSetupRequirement: String?): MilestoneDefinition =
        MilestoneDefinition(
            id,
            replaces,
            requirement,
            listOfNotNull(groupSetupRequirement, setupRequirement).joinToString().ifEmpty { null },
        )
  }

  // ACTIONS

  public fun readActions(json5: String): List<StandardActionDefinition> =
      fromJson5<ActionsImport>(json5).actions.map { it.complete() }

  @Serializable
  private data class ActionsImport(
      val actions: List<IncompleteActionDef>,
  ) {

    @Serializable
    data class IncompleteActionDef(
        val id: String,
        val action: String? = null,
        val actions: List<String>? = null,
        val setupRequirement: String? = null,
    ) {
      fun complete(): StandardActionDefinition {
        val realActions =
            if (action == null) {
              require(!actions.isNullOrEmpty())
              actions
            } else {
              require(actions == null)
              listOf(action)
            }
        return StandardActionDefinition(cn(id), realActions, setupRequirement)
      }
    }
  }

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
        val setupRequirement: String? = null,
        val rows: List<List<String>>,
    ) {
      internal fun toDefinition(
          legend: Legend,
      ): MarsMapDefinition {
        val mapName = cn(name)
        fun mapArea(
            row0Index: Int,
            col0Index: Int,
            code: String,
            legend: Legend,
        ): AreaDefinition? {
          val compactCode = code.filterNot { it.isWhitespace() }
          if (compactCode.isEmpty()) return null
          return AreaDefinition(
              mapName,
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
        return MarsMapDefinition(mapName, grid, setupRequirement?.let(::parse))
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

  private fun combineRequirements(first: String?, second: String?): String? =
      listOfNotNull(first, second).joinToString().ifEmpty { null }

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
