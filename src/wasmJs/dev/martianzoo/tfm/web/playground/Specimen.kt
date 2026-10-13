package dev.martianzoo.tfm.web.playground

import kotlinx.serialization.Serializable

/**
 * Display data read from the Solarnet world at a completed replay position. Pets strings are kept
 * exactly as the engine printed them; display shortening happens only when they are drawn.
 */
@Serializable
internal data class Specimen(
    val source: Source,
    val players: List<PlayerState>,
    val global: GlobalState,
    val milestones: List<Milestone>,
    val awards: List<Award>,
    val turmoil: Turmoil,
    val tasks: List<PendingTask>,
    val events: List<LoggedChange>,
    val components: List<PresentComponent>,
    val classes: List<ClassInfo>,
    val map: List<MapArea> = emptyList(),
    val colonies: List<ColonyState> = emptyList(),
) {
  val playerNames: List<String>
    get() = players.map { it.name }

  @Serializable internal data class Source(val replay: String, val event: Int)

  @Serializable
  internal data class PlayerState(
      val name: String,
      val terraformRating: Int,
      val handCards: Int,
      /** Stock and production for each standard resource. */
      val resources: Map<String, List<Int>>,
      val tags: Map<String, Int>,
      /** Face-up cards in the order they entered play. */
      val played: List<String>,
      /** The subset of [played] with local card art. */
      val playedWithArt: List<String>,
      val usedActionCards: Set<String> = emptySet(),
  )

  @Serializable
  internal data class GlobalState(
      val generation: Int,
      val phase: String?,
      val playerOnTurn: String?,
      val startingPlayer: String?,
      val temperatureSteps: Int,
      val oxygenSteps: Int,
      val oceans: Int,
      val venusSteps: Int,
      val ruling: String?,
      val dominant: String?,
      val chairman: String?,
      val currentGlobalEvent: String?,
      val comingGlobalEvent: String?,
      val distantGlobalEvent: String?,
  )

  @Serializable internal data class Milestone(val name: String, val claimedBy: String?)

  @Serializable internal data class Award(val name: String, val funded: Boolean)

  @Serializable
  internal data class Turmoil(
      val parties: List<String>,
      val delegates: List<Delegates>,
      val leaders: Map<String, String>,
  ) {
    @Serializable
    internal data class Delegates(val party: String, val owner: String, val count: Int)
  }

  @Serializable
  internal data class PendingTask(
      val id: Int,
      val instruction: String,
      val then: String?,
      val givenBy: String?,
      val because: Int?,
      val assignee: String,
      val selected: Boolean,
  ) {
    /** The instruction as the engine queues it, including any `THEN` continuation. */
    val fullInstruction: String
      get() = if (then == null) instruction else "$instruction THEN $then"
  }

  @Serializable
  internal data class LoggedChange(
      val ordinal: Int,
      val actor: String,
      val count: Int,
      val gaining: String?,
      val removing: String?,
      val real: Boolean,
      val signal: Boolean,
      val givenBy: String?,
      val because: Int?,
  )

  @Serializable
  internal data class PresentComponent(
      val type: String,
      val count: Int,
      val className: String,
      val nonReal: Boolean,
      val system: Boolean,
      /** Bound type of each dependency, keyed as `DeclaringClass_index`. */
      val dependencies: Map<String, String>,
  )

  @Serializable
  internal data class ClassInfo(
      val name: String,
      val abstract: Boolean,
      val superclasses: List<String>,
      /** The class with all of its dependencies at their declared bounds. */
      val baseType: String,
      val dependencyKeys: List<String>,
  )

  @Serializable
  internal data class MapArea(
      val row: Int,
      val column: Int,
      val kind: String,
      val tile: String?,
      val owner: String?,
      val bonus: List<MapBonus> = emptyList(),
  )

  @Serializable internal data class MapBonus(val image: String? = null, val label: String? = null)

  @Serializable
  internal data class ColonyState(
      val name: String,
      val production: Int,
      val owners: List<String>,
  )
}
