package dev.martianzoo.tfm.data

import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.Transforming.immediateToEffect
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.data.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.data.EnglishHack.englishHack
import dev.martianzoo.tfm.data.TfmClasses.ACTION_CARD
import dev.martianzoo.tfm.data.TfmClasses.ACTIVE_CARD
import dev.martianzoo.tfm.data.TfmClasses.AUTOMATED_CARD
import dev.martianzoo.tfm.data.TfmClasses.CARD_FRONT
import dev.martianzoo.tfm.data.TfmClasses.CORPORATION_CARD
import dev.martianzoo.tfm.data.TfmClasses.END
import dev.martianzoo.tfm.data.TfmClasses.EVENT_CARD
import dev.martianzoo.tfm.data.TfmClasses.PRELUDE_CARD
import dev.martianzoo.tfm.data.TfmClasses.PROJECT_CARD
import dev.martianzoo.tfm.data.TfmClasses.RESOURCE_CARD
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toSetStrict

/**
 * Everything there is to know about a Terraforming Mars card, except for text (including the card
 * name). It's theoretically possible to reconstruct acceptable instruction text from this data,
 * just not the original wording.
 */
public class CardDefinition(data: CardData) : Definition {
  /**
   * This card's unique id string. A number of id ranges, such as `"000"`-`"999"`, should be
   * reserved for canon (officially published) cards.
   */
  public val id: String by data::id

  override val shortName: ClassName = cn("C$id")

  override val className: ClassName = englishHack(id)

  override val bundle: String = data.bundle

  /**
   * Which deck this card belongs to, if any (i.e., Beginner Corporation does not). Note that this
   * property is public information even when the rest of the card data is hidden, then becomes
   * irrelevant as soon as the card has been played.
   */
  public val deck: Deck? = data.deck?.let(Deck::valueOf)

  /**
   * The id of the card this card replaces, if any. For example, the `"X31"` Deimos Down replaces
   * the `"039"` Deimos Down.
   */
  public val replaces: String? by data::replaces

  public val projectInfo: ProjectInfo? = if (deck == PROJECT) ProjectInfo(data) else null

  /**
   * The tags on the card. The list can contain duplicates (for example, Venus Governor has two
   * Venus tags). Order is irrelevant for gameplay purposes (canon data should preserve tag order
   * from printed cards just because).
   */
  public val tags: Multiset<ClassName> = HashMultiset.of(data.tags.map(::cn))

  /** Immediate effects on the card, if any. */
  public val immediate: Instruction? = data.immediate?.let(::parse)

  /**
   * Actions on the card, if any, each expressed as a PETS `Action`. `AUTOMATED` and `EVENT` cards
   * may not have these.
   */
  public val actions: List<Action> = data.actions.map(::parse)

  /**
   * Effects on the card, if any, each expressed as a PETS `Effect`. `AUTOMATED` and `EVENT` cards
   * may not have these.
   */
  public val effects: Set<Effect> = data.effects.toSetStrict(::parse)

  /** The card's requirement, if any. */
  public val requirement: Requirement? = projectInfo?.requirement

  /** The card's non-negative cost in megacredits. */
  public val cost: Int = projectInfo?.cost ?: 0

  /** Extra information that only project cards have. */
  public class ProjectInfo internal constructor(data: CardData) {
    public val kind: ProjectKind = ProjectKind.valueOf(data.projectKind!!)

    /** The card's requirement, if any. */
    public val requirement: Requirement? = data.requirement?.let(::parse)

    /** The card's non-negative cost in megacredits. */
    public val cost: Int by data::cost
  }

  /**
   * The type of `CardResource` this card can hold, if any. If this is non-null, then the class this
   * card is converted into will have a supertype of `ResourceCard<ThatResourceType>`. Of course,
   * that will fail if the class named here does not extend `CardResource`.
   */
  public val resourceType: ClassName? = data.resourceType?.let(::cn)

  init {
    if (deck == PROJECT) {
      val shouldBeActive =
          actions.any() ||
              effects.any { it.trigger != OnGainOf.create(END.expression) } ||
              resourceType != null
      require((projectInfo?.kind == ACTIVE) == shouldBeActive)
    }
  }

  /** Additional class declarations that come along with this card. */
  public val extraClasses: List<ClassDeclaration> = data.components.map(::parseOneLinerClass)

  override val asClassDeclaration by lazy {
    val createTags =
        Multi.create(tags.entries.map { (tag, count) -> gain(scaledEx(count, tag.of(THIS))) })

    val automaticFx: List<Effect> = listOfNotNull(immediateToEffect(createTags, true))

    val onPlayFx: List<Effect> =
        listOfNotNull(immediate).mapNotNull { immediateToEffect(it, false) }

    val allEffects: List<Effect> = automaticFx + onPlayFx + effects + actionListToEffects(actions)

    val supertypes =
        setOfNotNull(
                projectInfo?.kind?.className?.expression,
                resourceType?.let { RESOURCE_CARD.of(it.classExpression()) },
                if (actions.any()) ACTION_CARD.expression else null,
            )
            .ifEmpty { setOf(CARD_FRONT.expression) }

    ClassDeclaration(
        className = className,
        shortName = shortName,
        kind = CONCRETE,
        supertypes = supertypes,
        effects = allEffects.toSetStrict(),
        invariants = setOf(Max(scaledEx(1, className.expression))),
        extraNodes =
            setOfNotNull(requirement, deck?.className) + extraClasses.flatMap { it.allNodes },
    )
  }

  /** The deck this card belongs to; see [CardDefinition.deck]. */
  enum class Deck(val className: ClassName) {
    PROJECT(PROJECT_CARD),
    PRELUDE(PRELUDE_CARD),
    CORPORATION(CORPORATION_CARD)
  }

  /** A kind (color) of project; see [CardDefinition.ProjectInfo.kind]. */
  enum class ProjectKind(val className: ClassName) {
    EVENT(EVENT_CARD),
    AUTOMATED(AUTOMATED_CARD),
    ACTIVE(ACTIVE_CARD)
  }

  /** The *raw* imported form of a [CardDefinition]; not really meant to be widely consumed. */
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
        if (deck == "PRELUDE") immediate!!
        require(projectKind == null) { "not a project: $id" }
        require(requirement == null) { "can't have requirement: $id" }
        require(cost == 0) { "can't have nonzero cost: $id" }
      }
    }
  }
}
