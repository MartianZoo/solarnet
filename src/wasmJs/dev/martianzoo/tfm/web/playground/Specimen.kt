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
      val eventCards: Int,
      /** Stock and production for each standard resource. */
      val resources: Map<String, List<Int>>,
      val tags: Map<String, Int>,
      /** Face-up non-event cards in first-play order. */
      val played: List<PlayedCard>,
      /** Named cards in hand, known only in a live game. */
      val hand: List<String> = emptyList(),
      /** Named cards in the Selecting area, known only in a live game. */
      val selecting: List<String> = emptyList(),
  )

  @Serializable
  internal data class PlayedCard(
      val name: String,
      val kind: CardKind,
      val resources: Int?,
      val actionUsed: Boolean,
  )

  internal enum class CardKind {
    Corporation,
    Prelude,
    Automated,
    Active,
  }

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
      val iconic: Boolean,
      val signal: Boolean,
      val givenBy: String?,
      val because: Int?,
      val notes: String? = null,
      /**
       * Count after the event: gained type if present, otherwise removed type; blank for signals.
       */
      val balance: Int? = null,
  )

  @Serializable
  internal data class PresentComponent(
      val type: String,
      val count: Int,
      val className: String,
      val nonIconic: Boolean,
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
