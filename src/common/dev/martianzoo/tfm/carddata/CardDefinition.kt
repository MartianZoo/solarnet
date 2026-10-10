package dev.martianzoo.tfm.carddata

import kotlinx.serialization.Serializable

/**
 * Authored Terraforming Mars card data. Pets fragments remain literal source strings here.
 *
 * The card compiler always encloses the effects produced from [immediate], [actions], and [effects]
 * in OWN, including the rules of [components] and extracted inline classes. Explicit nested OWN is
 * allowed and never disables that wrapping. Thus a card's `Plant, OWN[Heat]` gives both resources
 * to its owner.
 *
 * [requirement] is stored as literal property syntax and transformed by PlayCard's OWN-marked use
 * of that property. [autoSelectWhen] and [invariants] are literal predicates; the compiler never
 * gives them implicit player scope. These policies depend on the field, never its text.
 */
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
        EVENT_TAG in tags -> EVENT_CARD
        actions.isNotEmpty() ||
            invariants.any { it.startsWith("=") } ||
            effects.any { !it.isScoringEffect() } -> ACTIVE_CARD
        else -> AUTOMATED_CARD
      }

  init {
    require(CARD_NAME.matches(name)) { "Invalid card name: $name" }
    require(deck == null || deck in CARD_DECKS) { "Invalid card deck: $deck" }
    require(tags.count { it == EVENT_TAG } <= 1) {
      "$EVENT_TAG must appear at most once in $name's tags"
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
private val CARD_DECKS = setOf("PreludeCard", "ProjectCard", "CorporationCard")
private const val PROJECT_DECK = "ProjectCard"
private const val EVENT_TAG = "EventTag"
private const val EVENT_CARD = "EventCard"
private const val ACTIVE_CARD = "ActiveCard"
private const val AUTOMATED_CARD = "AutomatedCard"

private fun String.isScoringEffect(): Boolean = startsWith("End:") || startsWith("End IF ")
