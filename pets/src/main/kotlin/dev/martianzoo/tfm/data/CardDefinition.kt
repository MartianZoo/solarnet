package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.data.SpecialClassNames.ACTION_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.ACTIVE_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.AUTOMATED_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.CARD_FRONT
import dev.martianzoo.tfm.data.SpecialClassNames.CORPORATION_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.EVENT_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.PRELUDE_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.PROJECT_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.RESOURCEFUL_CARD
import dev.martianzoo.tfm.pets.Parsing.parseOneLineClassDeclaration
import dev.martianzoo.tfm.pets.Parsing.parsePets
import dev.martianzoo.tfm.pets.SpecialClassNames.END
import dev.martianzoo.tfm.pets.actionsToEffects
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
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
    @Json(name = "id")
    val idRaw: String,

    /**
     * An optional textual identifier for the bundle this card belongs to, which can be used to
     * easily include or exclude sets of cards. The bundle ids `"A"`-`"Z"` are reserved for canon
     * (for example, `"B"` is "base", and `"R"` is "corporate era").
     */
    override val bundle: String,

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
     * The tags on the card, each expressed as a PETS class name. If a card (such as Venus
     * Governor) has multiple of the same tag, the same string should appear that many times in the
     * list. Order is irrelevant (but the Canon data preserves the tag order from the printed
     * cards).
     */
    @Json(name = "tags") val tagsText: List<String> = listOf(),

    /**
     * Immediate effects on the card, if any, each expressed as a PETS `Instruction`.
     */
    @Json(name = "immediate") val immediateText: String? = null,

    /**
     * Actions on the card, if any, each expressed as a PETS `Action`. `AUTOMATED` and `EVENT`
     * cards may not have these.
     */
    @Json(name = "actions") val actionsText: List<String> = listOf(),

    /**
     * Effects on the card, if any, each expressed as a PETS `Effect`. `AUTOMATED` and
     * `EVENT` cards may not have these.
     */
    @Json(name = "effects") val effectsText: Set<String> = setOf(),

    /**
     * Which resource type, if any, this card can hold, expressed as a PETS `TypeExpression`.
     */
    @Json(name = "resourceType") val resourceTypeText: String? = null,

    /**
     * Any extra classes the card defines (needed by no other card), expressed as a PETS
     * class declaration.
     */
    @Json(name = "components") val extraClassesText: Set<String> = setOf(),

    // Project info

    /**
     * The card's requirement, if it has one, expressed as a PETS `Requirement`. Only cards in
     * the `PROJECT` deck may have this.
     */
    @Json(name = "requirement") val requirementText: String? = null,

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
    require(idRaw.isNotEmpty())
    require(bundle.isNotEmpty())
    require(replaces?.isNotEmpty() ?: true)
    require(resourceTypeText?.isNotEmpty() ?: true)
    require(requirementText?.isNotEmpty() ?: true)

    when (deck) {
      Deck.PROJECT -> {
        require(cost >= 0)
        // a project should be ACTIVE iff it has persistent effects
        when (projectKind) {
          null -> error("Missing project kind: $idRaw")
          ACTIVE -> require(!inactive()) { "No persistent effects: $idRaw" }
          else -> require(inactive()) { "Persistent effects: $idRaw" }
        }
      }

      else -> {
        require(requirementText == null) { "can't have requirement: $idRaw" }
        require(cost == 0) { "can't have cost: $idRaw" }
        require(projectKind == null) { "not a project: $idRaw" }
      }
    }
  }

  override val id = ClassName("C$idRaw")
  override val name = englishHack(idRaw)

  // TODO ClassName
  val tags: List<TypeExpression> by lazy { tagsText.map(::gte) }

  val resourceType: ClassName? = resourceTypeText?.let(::ClassName)

  val immediateRaw: Instruction? by lazy {
    immediateText?.let { parsePets(it) }
  }
  val actionsRaw by lazy { actionsText.map { parsePets<Action>(it) } }
  val effectsRaw by lazy { effectsText.map { parsePets<Effect>(it) }.toSetStrict() }
  val requirementRaw: Requirement? by lazy { requirementText?.let { parsePets(it) } }

  // This doesn't get converted to an effect (yet??) so we have to canonicalize
  // TODO rethink
  val requirement by ::requirementRaw

  val allEffects: Set<Effect> by lazy {
    (listOfNotNull(immediateRaw).map(::immediateToEffect) +
        effectsRaw +
        actionsToEffects(actionsRaw)).toSetStrict()
  }

  val extraClasses: List<ClassDeclaration> by lazy {
    extraClassesText.map(::parseOneLineClassDeclaration)
  }

  override val asClassDeclaration by lazy {
    val supertypes = mutableSetOf<GenericTypeExpression>()

    projectKind?.let { supertypes += it.className.type }
    if (actionsRaw.any()) supertypes += ACTION_CARD.type
    resourceType?.let { supertypes += RESOURCEFUL_CARD.specialize(it.literal) }
    if (supertypes.isEmpty()) supertypes += CARD_FRONT.type

    ClassDeclaration(
        id = id,
        name = name,
        abstract = false,
        supertypes = supertypes,
        effectsRaw = allEffects,
        extraNodes = extraNodes)
  }

  private fun inactive(): Boolean {
    // do this low-tech to avoid unlazifying TODO
    return actionsText.isEmpty() &&
        effectsText.all { it.startsWith("$END:") } &&
        resourceTypeText == null
  }

  val extraNodes: Set<PetNode> by lazy {
      setOfNotNull(resourceType, requirementRaw, projectKind?.className, deck?.className) +
      tags +
      extraClasses.flatMap { it.allNodes }
  }

  /**
   * The deck this card belongs to; see [CardDefinition.deck].
   */
  enum class Deck(val className: ClassName) {
    PROJECT(PROJECT_CARD), PRELUDE(PRELUDE_CARD), CORPORATION(CORPORATION_CARD);
  }

  /**
   * A kind (color) of project; see [CardDefinition.projectKind].
   */
  enum class ProjectKind(val className: ClassName) { // TODO json adapter?
    EVENT(EVENT_CARD), AUTOMATED(AUTOMATED_CARD), ACTIVE(ACTIVE_CARD);
  }
}
