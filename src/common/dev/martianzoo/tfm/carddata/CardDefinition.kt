package dev.martianzoo.tfm.carddata

import kotlinx.serialization.Serializable

/** Pure authored data for one Terraforming Mars card. Pets fragments remain source strings. */
@Serializable
public data class CardDefinition(
    val name: String,
    val deck: String? = null,
    val requirement: String? = null,
    val cost: Int = 0,
    val autoSelectWhen: String? = null,
    val tags: List<String> = emptyList(),
    val immediate: String? = null,
    val actions: List<String> = emptyList(),
    val effects: List<String> = emptyList(),
    val invariants: Set<String> = emptySet(),
    val components: Set<String> = emptySet(),
) {
  /** The printed project-card category, derived from the authored rules that distinguish it. */
  public val projectKind: String? =
      when {
        deck != PROJECT_DECK -> null
        tags.lastOrNull() == EVENT_TAG -> EVENT_CARD
        actions.isNotEmpty() || effects.any { !it.isScoringEffect() } -> ACTIVE_CARD
        else -> AUTOMATED_CARD
      }

  init {
    require(CARD_NAME.matches(name)) { "Invalid card name: $name" }
    require(deck == null || deck in CARD_DECKS) { "Invalid card deck: $deck" }
    require(EVENT_TAG !in tags.dropLast(1)) {
      "$EVENT_TAG must appear once, at the end of $name's tags"
    }
    require(invariants.none(String::isEmpty))
    require(requirement?.isNotEmpty() != false)
    require(autoSelectWhen?.isNotEmpty() != false)
    require(cost >= 0)
    if (deck != PROJECT_DECK) {
      require(EVENT_TAG !in tags) { "Non-project card has $EVENT_TAG: $name" }
      require(requirement == null) { "Non-project card has a requirement: $name" }
      require(cost == 0) { "Non-project card has a cost: $name" }
    }
  }
}

private val CARD_NAME = Regex("[A-Za-z][A-Za-z0-9]*")
private val CARD_DECKS = setOf("PreludeCard", "ProjectCard", "StandardCorporationCard")
private const val PROJECT_DECK = "ProjectCard"
private const val EVENT_TAG = "EventTag"
private const val EVENT_CARD = "EventCard"
private const val ACTIVE_CARD = "ActiveCard"
private const val AUTOMATED_CARD = "AutomatedCard"

private fun String.isScoringEffect(): Boolean = startsWith("End:") || startsWith("End IF ")
