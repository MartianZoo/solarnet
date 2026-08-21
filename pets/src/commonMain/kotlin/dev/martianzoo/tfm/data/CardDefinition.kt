package dev.martianzoo.tfm.data

import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.DerivedClassLowerer
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.Transforming.immediateToEffect
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.data.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.data.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.data.TfmClasses.ACTION_CARD
import dev.martianzoo.tfm.data.TfmClasses.ACTIVE_CARD
import dev.martianzoo.tfm.data.TfmClasses.AUTOMATED_CARD
import dev.martianzoo.tfm.data.TfmClasses.CARD_FRONT
import dev.martianzoo.tfm.data.TfmClasses.CORPORATION_CARD
import dev.martianzoo.tfm.data.TfmClasses.END
import dev.martianzoo.tfm.data.TfmClasses.EVENT_CARD
import dev.martianzoo.tfm.data.TfmClasses.EVENT_TAG
import dev.martianzoo.tfm.data.TfmClasses.PRELUDE_CARD
import dev.martianzoo.tfm.data.TfmClasses.PROJECT_CARD
import dev.martianzoo.tfm.data.TfmClasses.RESOURCE_CARD
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import kotlinx.serialization.Serializable

/**
 * Everything there is to know about a Terraforming Mars card except its localized display text.
 * It's theoretically possible to reconstruct acceptable instruction text from this data, just not
 * the original wording.
 */
public class CardDefinition(data: CardData) : Definition {
  override val className: ClassName = cn(data.name)

  private val derivedClasses = DerivedClassLowerer(className)

  private inline fun <reified P : PetNode> parseOwned(source: String): P =
      Parsing.parse(P::class, source, derivedClasses)

  /**
   * Which deck this card belongs to, if any (i.e., Beginner Corporation does not). Note that this
   * property is public information even when the rest of the card data is hidden, then becomes
   * irrelevant as soon as the card has been played.
   */
  public val deck: Deck? = data.deck?.let(Deck::valueOf)

  /** The card this card replaces, if any. For example, `DeimosDownPromo` replaces `DeimosDown`. */
  public val replaces: ClassName? = data.replaces?.let(::cn)

  /** Configuration condition that must hold for this card to be active. */
  override val setupRequirement: Requirement? = data.setupRequirement?.let(::parseOwned)

  public val projectInfo: ProjectInfo? =
      if (deck == PROJECT) {
        ProjectInfo(data, data.requirement?.let(::parseOwned))
      } else {
        null
      }

  /**
   * The tags on the card. The list can contain duplicates (for example, Venus Governor has two
   * Venus tags). Order is irrelevant for gameplay purposes (canon data should preserve tag order
   * from printed cards just because). Every event card additionally receives the derived
   * [EVENT_TAG].
   */
  public val tags: Multiset<ClassName> =
      HashMultiset.of(
          data.tags.map(::cn).also { authoredTags ->
            require(EVENT_TAG !in authoredTags) {
              "EventTag is derived from projectKind: $className"
            }
          } + listOfNotNull(if (projectInfo?.kind == ProjectKind.EVENT) EVENT_TAG else null)
      )

  /** The card's immediate instruction, if any. */
  public val immediate: InstructionGroup? =
      data.immediate?.let {
        InstructionGroup.of(parseOwned<InstructionTree>(it))
      }

  /**
   * Actions on the card, if any, each expressed as a PETS `Action`. `AUTOMATED` and `EVENT` cards
   * may not have these.
   */
  public val actions: List<Action> = data.actions.map(::parseOwned)

  /**
   * Effects on the card, if any, each expressed as a PETS `Effect`. `AUTOMATED` and `EVENT` cards
   * may not have these.
   */
  public val effects: List<Effect> = data.effects.map(::parseOwned)

  /** The card's printed play requirement, if any. */
  public val requirement: Requirement? = projectInfo?.requirement

  /** The card's non-negative cost in megacredits. */
  public val cost: Int = projectInfo?.cost ?: 0

  /** Extra information that only project cards have. */
  public class ProjectInfo internal constructor(data: CardData, requirement: Requirement?) {
    public val kind: ProjectKind = ProjectKind.valueOf(data.projectKind!!)

    /** The card's printed play requirement, if any. */
    public val requirement: Requirement? = requirement

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

  private val componentClasses: List<ClassDeclaration> = data.components.map(::parseOneLinerClass)

  /** Additional class declarations that come along with this card. */
  public val extraClasses: List<ClassDeclaration> =
      componentClasses + derivedClasses.declarations + listOfNotNull(resourceClassDeclaration())

  override val asClassDeclaration: ClassDeclaration by lazy {
    val createTags =
        InstructionGroup.createTree(tags.entries.map { (tag, count) -> gain(tag.of(THIS), count) })

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
        kind = CONCRETE,
        supertypes = supertypes,
        effects = allEffects,
        invariants = setOf(Max(scaledEx(className.expression, 1))),
        properties =
            buildMap {
              put(COST_PROPERTY, NumberValue(cost))
              requirement?.let { put(REQUIREMENT_PROPERTY, RequirementValue(it)) }
            },
        extraNodes =
            setOfNotNull(deck?.className) + componentClasses.map(ClassDeclaration::className),
    )
  }

  /** The deck this card belongs to; see [CardDefinition.deck]. */
  public enum class Deck(public val className: ClassName) {
    PROJECT(PROJECT_CARD),
    PRELUDE(PRELUDE_CARD),
    CORPORATION(CORPORATION_CARD),
  }

  /** A kind (color) of project; see [CardDefinition.ProjectInfo.kind]. */
  public enum class ProjectKind(internal val className: ClassName) {
    EVENT(EVENT_CARD),
    AUTOMATED(AUTOMATED_CARD),
    ACTIVE(ACTIVE_CARD),
  }

  /** The *raw* imported form of a [CardDefinition]; not really meant to be widely consumed. */
  @Serializable
  public data class CardData(
      val name: String,
      val deck: String? = null,
      val replaces: String? = null,
      val setupRequirement: String? = null,
      val tags: List<String> = emptyList(),
      val immediate: String? = null,
      val actions: List<String> = emptyList(),
      val effects: List<String> = emptyList(),
      val resourceType: String? = null,
      val components: Set<String> = emptySet(),
      val requirement: String? = null,
      val cost: Int = 0,
      val projectKind: String? = null,
  ) {
    init {
      cn(name)
      require(replaces?.isNotEmpty() != false)
      require(setupRequirement?.isNotBlank() != false)
      require(resourceType?.isNotEmpty() != false)
      require(requirement?.isNotEmpty() != false)
      require(cost >= 0)

      if (deck == "PROJECT") {
        require(projectKind != null)
      } else {
        require(projectKind == null) { "not a project: $name" }
        require(requirement == null) { "can't have requirement: $name" }
        require(cost == 0) { "can't have nonzero cost: $name" }
      }
    }
  }

  private fun resourceClassDeclaration(): ClassDeclaration? = resourceType?.let { type ->
    parseOneLinerClass("CLASS $type : CardResource<ResourceHolder<Class<$type>, Owner>>")
  }

  private companion object {
    val COST_PROPERTY = PropertyName("cost")
    val REQUIREMENT_PROPERTY = PropertyName("requirement")
  }
}
