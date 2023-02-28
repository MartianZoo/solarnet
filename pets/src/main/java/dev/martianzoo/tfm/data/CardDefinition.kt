package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.END
import dev.martianzoo.tfm.data.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.data.EnglishHack.englishHack
import dev.martianzoo.tfm.data.SpecialClassNames.ACTION_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.ACTIVE_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.AUTOMATED_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.CARD_FRONT
import dev.martianzoo.tfm.data.SpecialClassNames.CORPORATION_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.EVENT_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.PRELUDE_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.PROJECT_CARD
import dev.martianzoo.tfm.data.SpecialClassNames.RESOURCEFUL_CARD
import dev.martianzoo.tfm.pets.AstTransforms.actionListToEffects
import dev.martianzoo.tfm.pets.AstTransforms.immediateToEffect
import dev.martianzoo.tfm.pets.Parsing.parseOneLineClassDeclaration
import dev.martianzoo.tfm.pets.ast.Action.Companion.action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Companion.effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.util.toSetStrict

/**
 * Everything there is to know about a Terraforming Mars card, except for text (including the card
 * name). It's theoretically possible to reconstruct acceptable instruction text from this data,
 * just not the original wording.
 */
public class CardDefinition(data: CardData) : Definition {
  /**
   * This card's unique id string, prefixed with `C` (for "card"). A number of id ranges, such as
   * `"C000"`-`"C999"`, are reserved for canon (officially published) cards. (TODO)
   */
  override val id: ClassName = cn("C${data.id}")

  override val className: ClassName = englishHack(id)

  override val bundle: String = data.bundle

  /**
   * The tags on the card. The list can contain duplicates (for example, Venus Governor has two
   * Venus tags). Order is irrelevant for gameplay purposes (canon data should preserve tag order
   * from printed cards just because).
   */
  val tags: List<ClassName> = data.tags.map(::cn)

  /**
   * The type of `CardResource` this card can hold, if any. If this is non-null, then the class this
   * card is converted into will have a supertype of `ResourcefulCard<ThatResourceType>`. Of course,
   * that will fail if the class named here does not extend `CardResource`.
   */
  val resourceType: ClassName? = data.resourceType?.let(::cn)

  // TODO unlazify

  /** Immediate effects on the card, if any. */
  val immediate: Instruction? = data.immediate?.let(::instruction)

  /**
   * Actions on the card, if any, each expressed as a PETS `Action`. `AUTOMATED` and `EVENT` cards
   * may not have these.
   */
  val actions = data.actions.map(::action).toSetStrict()

  /**
   * Effects on the card, if any, each expressed as a PETS `Effect`. `AUTOMATED` and `EVENT` cards
   * may not have these.
   */
  val effects = data.effects.map(::effect).toSetStrict()

  // TODO ProjectInfo

  /** The card's requirement, if it has one. Only cards in the `PROJECT` deck may have this. */
  val requirement: Requirement? = data.requirement?.let(::requirement)

  /** The card's non-negative cost in megacredits. Is only nonzero for Project cards. */
  val cost: Int = data.cost

  /**
   * What kind of project this is, or `null` if it is not a project. Even though Prelude and
   * Corporation cards act exactly like `AUTOMATED` and `ACTIVE` cards, respectively, they
   * officially don't count as such.
   */
  val projectKind: ProjectKind? = data.projectKind?.let(ProjectKind::valueOf)

  /**
   * Which deck this card belongs to, if any (i.e., Beginner Corporation does not). Note that this
   * property is public information even when the rest of the card data is hidden, then becomes
   * irrelevant as soon as the card has been played.
   */
  val deck: Deck? = data.deck?.let(Deck::valueOf)

  /**
   * The id of the card this card replaces, if any. For example, the `"X31"` Deimos Down replaces
   * the `"039"` Deimos Down.
   */
  val replaces: String? = data.replaces

  init {
    if (deck == PROJECT) {
      val active =
          actions.any() ||
          effects.any { it.trigger != OnGainOf.create(END.type) } ||
          resourceType != null
      require(active == (projectKind == ACTIVE))
    }
  }

  /** Additional class declarations that come along with this card. */
  val extraClasses: List<ClassDeclaration> =
      data.components.map { parseOneLineClassDeclaration(it) }

  override val asClassDeclaration by lazy {
    val supertypes = mutableSetOf<TypeExpr>()

    projectKind?.let { supertypes += it.className.type }
    if (actions.any()) supertypes += ACTION_CARD.type
    resourceType?.let { supertypes += RESOURCEFUL_CARD.addArgs(CLASS.addArgs(it)) }
    if (supertypes.isEmpty()) supertypes += CARD_FRONT.type

    val allEffects =
        listOfNotNull(immediate).map(::immediateToEffect) + effects + actionListToEffects(actions)

    ClassDeclaration(
        name = className,
        id = id,
        abstract = false,
        supertypes = supertypes,
        effectsRaw = allEffects.toSetStrict(),
        extraNodes = extraNodes)
  }

  val extraNodes: Set<PetNode> by lazy {
    setOfNotNull(resourceType, requirement, projectKind?.className, deck?.className) +
        tags +
        extraClasses.flatMap { it.allNodes }
  }

  /** The deck this card belongs to; see [CardDefinition.deck]. */
  enum class Deck(val className: ClassName) {
    PROJECT(PROJECT_CARD),
    PRELUDE(PRELUDE_CARD),
    CORPORATION(CORPORATION_CARD)
  }

  /** A kind (color) of project; see [CardDefinition.projectKind]. */
  enum class ProjectKind(val className: ClassName) { // TODO json adapter?
    EVENT(EVENT_CARD),
    AUTOMATED(AUTOMATED_CARD),
    ACTIVE(ACTIVE_CARD)
  }

  /** The raw imported form of a [CardDefinition]. */
  public data class CardData(
      val id: String,
      val bundle: String,
      val deck: String? = null,
      val replaces: String? = null,
      val tags: List<String> = listOf(),
      val immediate: String? = null,
      val actions: List<String> = listOf(),
      val effects: Set<String> = setOf(),
      val resourceType: String? = null,
      val components: Set<String> = setOf(),
      val requirement: String? = null,
      val cost: Int = 0,
      val projectKind: String? = null,
  ) {
    init {
      require(id.isNotEmpty())
      require(bundle.isNotEmpty())
      require(replaces?.isNotEmpty() ?: true)
      require(resourceType?.isNotEmpty() ?: true)
      require(requirement?.isNotEmpty() ?: true)
      require(cost >= 0)

      if (deck == "PROJECT") {
        require(projectKind != null)
      } else {
        require(projectKind == null) { "not a project: $id" }
        require(requirement == null) { "can't have requirement: $id" }
        require(cost == 0) { "can't have nonzero cost: $id" }
      }
    }
  }
}
