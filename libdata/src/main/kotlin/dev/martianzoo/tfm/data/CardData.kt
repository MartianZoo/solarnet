package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.CardData.ProjectKind.ACTIVE

/**
 * Everything there is to know about a Terraforming Mars card, except for text (including the card
 * name). It's theoretically possible to reconstruct acceptable instruction text from this data,
 * just not the original wording.
 */
data class CardData(
    /**
     * This card's unique id string: its printed id if it has one; otherwise the one we made up. A
     * number of id ranges, such as `"000"`-`"999"`, are reserved for canon (officially published)
     * cards.
     *
     * **Validation:** the properties state numerous "guarantees", but in most cases this class is
     * not able to internally validate those conditions. Actual parsing of the strings must be
     * handled by a Petaform parser external to this library.
     *
     * If a card (like Deimos Down) has multiple variants which are meant to never coexist, each
     * variant still needs its own unique id. The `replacesId` property for the replacement card
     * will identify the replaced card.
     *
     * It is of course possible for non-canon cards to have colliding ids, which would prevent them
     * from being used simultaneously.
     */
    val id: String,

    /**
     * An optional textual identifier for the bundle this card belongs to, which can be used to
     * easily include or exclude sets of cards. The bundle ids `"A"`-`"Z"` are reserved for canon
     * (for example, `"B"` is "base", and `"R"` is "corporate era").
     */
    val bundle: String? = null,

    /**
     * Which deck this card belongs to, if any (i.e., Beginner Corporation does not). Note that this
     * property is public information even when the rest of the card data is hidden, then becomes
     * irrelevant as soon as the card has been played.
     */
    val deck: Deck? = null,

    /**
     * The tags on the card, each expressed as a Petaform component name. If a card (such as Venus
     * Governor) has multiple of the same tag, the same string should appear that many times in the
     * list. Order is irrelevant, but should ideally match the order on the printed card (if it
     * exists).
     */
    val tags: List<String> = listOf(),

    /**
     * *All* the game behaviors of the card, each represented as a Petaform triggered effect (with
     * `":"`) or manual action (with `"->"`). `AUTOMATED` and `EVENT` projects may have only `This:`
     * effects.
     */
    val effects: List<String> = listOf(),

    /**
     * The id of the card this card replaces, if any. For example, the `"X31"` Deimos Down replaces
     * the `"039"` Deimos Down.
     */
    val replacesId: String? = null,

    /**
     * Which resource type, if any, this card can hold, expressed as a Petaform component name.
     */
    val resourceType: String? = null,

    // Project info

    /**
     * The card's requirement, expressed as a nonempty Petaform predicate, or `null` if it has none.
     * Is only non-null for Project cards.
     */
    val requirement: String? = null,

    /**
     * The card's nonnegative cost in megacredits. Is only nonzero for Project cards.
     */
    val cost: Int = 0,

    /**
     * What kind of project this is, or `null` if it is not a project. Even though Prelude and
     * Corporation cards act exactly like `AUTOMATED` and `ACTIVE` cards, respectively, they
     * officially don't count as such.
     */
    val projectKind: ProjectKind? = null,
) {

  init {
    require(id.isNotEmpty())
    require(bundle?.isNotEmpty() ?: true)
    require(replacesId?.isNotEmpty() ?: true)
    require(resourceType?.isNotEmpty() ?: true)
    require(requirement?.isNotEmpty() ?: true)

    when (deck) {
      Deck.PROJECT -> {
        require(cost >= 0)
        require(projectKind != null)
        require(projectKind == ACTIVE || effects.all { it.startsWith("This:") })
      }
      else -> {
        require(requirement == null)
        require(cost == 0)
        require(projectKind == null)
      }
    }
  }

  /**
   * The deck this card belongs to; see [CardData.deck].
   */
  enum class Deck { PROJECT, PRELUDE, CORPORATION }

  /**
   * A kind (color) of project; see [CardData.projectKind].
   */
  enum class ProjectKind {
    EVENT, // red
    AUTOMATED, // green
    ACTIVE // blue
  }
}
