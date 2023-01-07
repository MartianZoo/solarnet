package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.pets.PetsParser
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.SpecialComponent.END
import dev.martianzoo.tfm.pets.actionsToEffects
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.immediateToEffect
import dev.martianzoo.util.toSetStrict

/**
 * Everything there is to know about a Terraforming Mars card, except for text (including the card
 * name). It's theoretically possible to reconstruct acceptable instruction text from this data,
 * just not the original wording.
 *
 * *Validation:*  the properties state numerous "guarantees", but in most cases this class is not
 * able to internally validate those conditions. Actual parsing of the strings must be handled by a
 * PETS parser external to this library.
 */
data class CardDefinition(
    /**
     * This card's unique id string: its printed id if it has one; otherwise the one we made up. A
     * number of id ranges, such as `"000"`-`"999"`, are reserved for canon (officially published)
     * cards.
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
     * The id of the card this card replaces, if any. For example, the `"X31"` Deimos Down replaces
     * the `"039"` Deimos Down.
     */
    val replaces: String? = null,

    /**
     * The tags on the card, each expressed as a PETS component name. If a card (such as Venus
     * Governor) has multiple of the same tag, the same string should appear that many times in the
     * list. Order is irrelevant (but the Canon data preserves the tag order from the printed
     * cards).
     */
    @Json(name = "tags")
    val tagsText: List<String> = listOf(),

    /**
     * Immediate effects on the card, if any, each expressed as a PETS `Instruction`.
     */
    @Json(name = "immediate")
    val immediateText: Set<String> = setOf(), // TODO there should be only one

    /**
     * Actions on the card, if any, each expressed as a PETS `Action`. `AUTOMATED` and `EVENT`
     * cards may not have these.
     */
    @Json(name = "actions")
    val actionsText: Set<String> = setOf(), // TODO change to list

    /**
     * Effects on the card, if any, each expressed as a PETS `Effect`. `AUTOMATED` and
     * `EVENT` cards may not have these.
     */
    @Json(name = "effects")
    val effectsText: Set<String> = setOf(),

    /**
     * Which resource type, if any, this card can hold, expressed as a PETS `TypeExpression`.
     */
    @Json(name = "resourceType")
    val resourceTypeText: String? = null,

    /**
     * Any extra components the card defines (needed by no other card), expressed as a PETS
     * `Component`.
     */
    @Json(name = "components")
    val extraComponentsText: Set<String> = setOf(),

    // Project info

    /**
     * The card's requirement, if it has one, expressed as a PETS `Requirement`. Only cards in
     * the `PROJECT` deck may have this.
     */
    @Json(name = "requirement")
    val requirementText: String? = null,

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
) : Definition {

  init {
    require(id.isNotEmpty())
    require(bundle?.isNotEmpty() ?: true)
    require(replaces?.isNotEmpty() ?: true)
    require(resourceTypeText?.isNotEmpty() ?: true)
    require(requirementText?.isNotEmpty() ?: true)

    when (deck) {
      Deck.PROJECT -> {
        require(cost >= 0)
        // a project should be ACTIVE iff it has persistent effects
        when (projectKind) {
          null -> error("Missing project kind: $id")
          ACTIVE -> require(!inactive()) { "No persistent effects: $id" }
          else -> require(inactive()) { "Persistent effects: $id" }
        }
      }
      else -> {
        require(requirementText == null) { "can't have requirement: $id" }
        require(cost == 0) { "can't have cost: $id" }
        require(projectKind == null) { "not a project: $id" }
      }
    }
  }

  override val className = englishHack(id)

  // TODO ClassName
  val tags: List<TypeExpression> by lazy { tagsText.map(::te) }

  val resourceType = resourceTypeText?.let(::ClassExpression)

  val immediateRaw: Instruction? by lazy {
    val set = immediateText.map { parse<Instruction>(it) }.toSetStrict()
    when (set.size) {
      0 -> null
      1 -> set.first()
      else -> {
        Multi(set.flatMap {
          if (it is Multi) it.instructions else listOf(it)
        })
      }
    }
  }
  val actionsRaw by lazy { actionsText.map { parse<Action>(it) }.toSetStrict() }
  val effectsRaw by lazy { effectsText.map { parse<Effect>(it) }.toSetStrict() }
  val requirementRaw: Requirement? by lazy { requirementText?.let(PetsParser::parse) }

  // This doesn't get converted to an effect (yet??) so we have to canonicalize
  // TODO rethink
  val requirement by ::requirementRaw

  val allEffects: Set<Effect> by lazy {
    (listOfNotNull(immediateRaw).map(::immediateToEffect) +
        effectsRaw +
        actionsToEffects(actionsRaw)).toSetStrict()
  }

  override val asClassDeclaration by lazy {
    val supertypes = mutableSetOf<GenericTypeExpression>()

    if (projectKind != null)   supertypes.add(te(projectKind.type))
    if (!actionsRaw.isEmpty()) supertypes.add(te("ActionCard"))
    if (resourceType != null)  supertypes.add(te("ResourcefulCard", resourceType))
    if (supertypes.isEmpty())  supertypes.add(te("CardFront"))

    ClassDeclaration(
        className = className,
        abstract = false,
        supertypes = supertypes,
        effectsRaw = allEffects)
  }

  private fun inactive(): Boolean {
    // do this low-tech to avoid unlazifying
    return actionsText.isEmpty() &&
        effectsText.all { it.startsWith("$END:") } &&
        resourceTypeText == null
  }

  /**
   * The deck this card belongs to; see [CardDefinition.deck].
   */
  enum class Deck { PROJECT, PRELUDE, CORPORATION }

  /**
   * A kind (color) of project; see [CardDefinition.projectKind].
   */
  enum class ProjectKind(val type: String) {
    EVENT("EventCard"), // red
    AUTOMATED("AutomatedCard"), // green
    ACTIVE("ActiveCard") // blue
  }
}
