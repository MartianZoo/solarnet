package dev.martianzoo.tfm.carddata

import kotlinx.serialization.Serializable

/**
 * Authored fields for a single card before Pets generation. Pets fragments are retained as source
 * text; constructing a definition validates its name, deck, and the cross-field restrictions below,
 * but does not parse those fragments.
 *
 * A project card must name `ProjectCard` as its [deck]. Only project cards may have a
 * [requirement], a positive [cost], or an `EventTag`. An absent deck uses the generic `CardBack`.
 * Empty collections mean that the corresponding Pets declarations are absent.
 *
 * @property name Pets class name for the card; starts with a letter and contains only ASCII letters
 *   and digits.
 * @property deck `PreludeCard`, `ProjectCard`, or `CorporationCard`, or null for generic
 *   `CardBack`.
 * @property requirement Optional nonempty Pets play requirement for a project card.
 * @property cost Nonnegative printed MC cost of a project card; zero when absent or not a project.
 * @property autoSelectWhen Optional nonempty Pets requirement stored as the card's automatic
 *   selection condition.
 * @property tags Authored tag names; `EventTag` occurs at most once and only on a project card.
 * @property direct Optional Pets instruction performed when the card enters play; null supplies no
 *   direct effect.
 * @property actions Authored Pets action fragments, in source order.
 * @property effects Authored Pets effect fragments, in source order.
 * @property invariants Nonempty Pets requirements attached to the card declaration.
 * @property components Pets one-line component class declarations supplied by the card.
 * @throws IllegalArgumentException if a field or combination violates the stated restrictions.
 */
@Serializable
public data class CardDefinition(
    val name: String,
    val deck: String? = null,
    val requirement: String? = null,
    val cost: Int = 0,
    val autoSelectWhen: String? = null,
    /** Card whose automatic selection yields to this card; explicit selection remains possible. */
    val replaces: String? = null,
    val tags: List<String> = emptyList(),
    val direct: String? = null,
    val actions: List<String> = emptyList(),
    val effects: List<String> = emptyList(),
    val invariants: Set<String> = emptySet(),
    val components: Set<String> = emptySet(),
) {
  /**
   * Printed project-card category. Null for other decks; `EventCard` when tagged `EventTag`;
   * otherwise `ActiveCard` when the card has an action, an invariant starting with `=`, or a
   * non-scoring effect. The remaining project cards are `AutomatedCard`. Effects starting with
   * `End:` or `End IF ` count as scoring effects for this classification. For example,
   * `CardDefinition(name = "Demo", deck = "ProjectCard").projectKind` is `AutomatedCard`; adding an
   * action makes it `ActiveCard`, while adding `EventTag` makes it `EventCard` even if it also has
   * an action.
   */
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
    require(replaces == null || CARD_NAME.matches(replaces)) { "Invalid replaced card: $replaces" }
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
