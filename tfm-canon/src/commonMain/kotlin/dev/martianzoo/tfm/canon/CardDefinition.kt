package dev.martianzoo.tfm.canon

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.DerivedClassLowerer
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.Transforming.actionSelectors
import dev.martianzoo.pets.Transforming.immediateToEffect
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.canon.CardDefinition.Deck.PROJECT
import dev.martianzoo.tfm.canon.CardDefinition.ProjectKind.ACTIVE
import dev.martianzoo.tfm.canon.TfmClasses.ACTION_CARD
import dev.martianzoo.tfm.canon.TfmClasses.ACTIVE_CARD
import dev.martianzoo.tfm.canon.TfmClasses.AUTOMATED_CARD
import dev.martianzoo.tfm.canon.TfmClasses.CARD_FRONT
import dev.martianzoo.tfm.canon.TfmClasses.CARD_RESOURCE
import dev.martianzoo.tfm.canon.TfmClasses.CORPORATION_CARD
import dev.martianzoo.tfm.canon.TfmClasses.END
import dev.martianzoo.tfm.canon.TfmClasses.EVENT_CARD
import dev.martianzoo.tfm.canon.TfmClasses.EVENT_TAG
import dev.martianzoo.tfm.canon.TfmClasses.PRELUDE_CARD
import dev.martianzoo.tfm.canon.TfmClasses.PROJECT_CARD
import dev.martianzoo.tfm.canon.TfmClasses.RESOURCE_CARD
import dev.martianzoo.tfm.canon.TfmClasses.TAG
import dev.martianzoo.types.Class as PetClass
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import kotlinx.serialization.Serializable

/**
 * Everything there is to know about a Terraforming Mars card except its localized display text.
 * It's theoretically possible to reconstruct acceptable instruction text from this data, just not
 * the original wording.
 */
public class CardDefinition
private constructor(
    private val data: CardData,
    private val loadedClass: PetClass?,
) : Definition {
  public constructor(data: CardData) : this(data, null)

  override val className: ClassName = cn(data.name)

  private val derivedClasses = DerivedClassLowerer(className)

  private inline fun <reified P : PetNode> parseOwned(source: String): P =
      Parsing.parse(P::class, source, derivedClasses)

  /**
   * Which deck this card belongs to, if any (i.e., Beginner Corporation does not). Note that this
   * property is public information even when the rest of the card data is hidden, then becomes
   * irrelevant as soon as the card has been played.
   */
  private val sourceDeck: Deck? = data.deck?.let(Deck::valueOf)

  public val deck: Deck?
    get() =
        loadedClass?.representedCardClass()?.let { represented ->
          Deck.entries.singleOrNull { it.className == represented.className }
        } ?: sourceDeck

  override val automaticSelectionRequirement: Requirement? =
      if (sourceDeck == Deck.PRELUDE) Parsing.parse("PreludeDeck") else null

  /** The card this card replaces, if any. For example, `DeimosDownPromo` replaces `DeimosDown`. */
  public val replaces: ClassName? = data.replaces?.let(::cn)

  internal val projectInfo: ProjectInfo? =
      if (sourceDeck == PROJECT) {
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
  private val sourceTags: Multiset<ClassName> =
      HashMultiset.of(
          data.tags.map(::cn).also { authoredTags ->
            require(EVENT_TAG !in authoredTags) {
              "EventTag is derived from projectKind: $className"
            }
          } + listOfNotNull(if (projectInfo?.kind == ProjectKind.EVENT) EVENT_TAG else null)
      )

  public val tags: Multiset<ClassName>
    get() = loadedClass?.loadedTags() ?: sourceTags

  /** Authored Pets source for the card's immediate instruction, if any. */
  public val authoredImmediateSource: String?
    get() = data.immediate

  private val sourceImmediate: InstructionGroup? =
      data.immediate?.let {
        InstructionGroup.of(parseOwned<InstructionTree>(it))
      }

  public val immediate: InstructionGroup?
    get() = loadedClass?.loadedImmediate() ?: sourceImmediate

  /** Authored Pets source for the card's actions. */
  public val authoredActionSources: List<String>
    get() = data.actions

  private val sourceActions: List<Action> = data.actions.map(::parseOwned)

  public val actions: List<Action>
    get() = loadedClass?.declaration?.authoredActions ?: sourceActions

  /** Authored Pets source for the card's effects. */
  public val authoredEffectSources: List<String>
    get() = data.effects

  private val sourceEffects: List<Effect> = data.effects.map(::parseOwned)

  public val effects: List<Effect>
    get() = loadedClass?.loadedAuthoredEffects() ?: sourceEffects

  private val componentClasses: List<ClassDeclaration> = data.components.map(::parseOneLinerClass)

  /** The card's printed play requirement, if any. */
  public val requirement: Requirement?
    get() =
        loadedClass?.properties?.get(REQUIREMENT_PROPERTY)?.let { value ->
          (value as? RequirementValue)?.value
        } ?: projectInfo?.requirement

  /** The card's non-negative cost in megacredits. */
  public val cost: Int
    get() =
        loadedClass?.properties?.get(COST_PROPERTY)?.let { value ->
          (value as NumberValue).value
        } ?: projectInfo?.cost ?: 0

  /** Extra information that only project cards have. */
  internal class ProjectInfo internal constructor(data: CardData, requirement: Requirement?) {
    internal val kind: ProjectKind = ProjectKind.valueOf(data.projectKind!!)

    /** The card's printed play requirement, if any. */
    public val requirement: Requirement? = requirement

    /** The card's non-negative cost in megacredits. */
    public val cost: Int by data::cost
  }

  /** Class names whose authored use suggests that this card holds that kind of component. */
  public val resourceTypeCandidates: Set<ClassName> = deriveResourceTypeCandidates()

  private val sourceResourceType: ClassName? = resourceTypeCandidates.singleOrNull()

  public val resourceType: ClassName?
    get() = loadedClass?.representedResourceClass()?.className ?: sourceResourceType

  init {
    if (sourceDeck == PROJECT) {
      val shouldBeActive =
          sourceActions.any() ||
              sourceEffects.any { it.trigger != OnGainOf.create(END.expression) } ||
              sourceResourceType != null
      require((projectInfo?.kind == ACTIVE) == shouldBeActive)
    }
  }

  /** Supporting declarations emitted with generated Pets source for this card. */
  public val authoredSupportingClasses: List<ClassDeclaration> = componentClasses

  /** Additional class declarations that come along with this card. */
  public val extraClasses: List<ClassDeclaration> = componentClasses + derivedClasses.declarations

  /** Follow-mode declarations with source-level real-card operations compiled. */
  internal val executableExtraClasses: List<ClassDeclaration> =
      extraClasses.map(FollowModeNeutralizer::neutralize)

  override val asClassDeclaration: ClassDeclaration by lazy {
    toClassDeclaration(sourceResourceType)
  }

  internal fun toClassDeclaration(resourceType: ClassName?): ClassDeclaration {
    return loadedClass?.declaration
        ?: run {
          val createTags =
              InstructionGroup.createTree(
                  sourceTags.entries.map { (tag, count) ->
                    gain(tag.of(THIS), count, intensity = null)
                  }
              )

          val automaticFx: List<Effect> = listOfNotNull(immediateToEffect(createTags, true))

          val onPlayFx: List<Effect> =
              listOfNotNull(sourceImmediate).mapNotNull { immediateToEffect(it, false) }

          val authoredEffects: List<Effect> = automaticFx + onPlayFx + sourceEffects
          val allEffects: List<Effect> = authoredEffects + actionListToEffects(sourceActions)

          val cardBackClass = (sourceDeck?.className ?: CARD_BACK).classExpression()
          val cardRolesBySpecificity =
              listOfNotNull(
                  projectInfo?.kind?.className?.expression,
                  resourceType?.let { RESOURCE_CARD.of(it.classExpression()) },
                  ACTION_CARD.expression.takeIf { sourceActions.any() },
              )
          val supertypes = buildSet {
            if (cardRolesBySpecificity.isEmpty()) {
              add(CARD_FRONT.of(cardBackClass))
            } else {
              add(cardRolesBySpecificity.first().appendArguments(listOf(cardBackClass)))
              addAll(cardRolesBySpecificity.drop(1))
            }
          }

          ClassDeclaration(
              className = className,
              kind = CONCRETE,
              supertypes = supertypes,
              authoredEffects = authoredEffects,
              authoredActions = sourceActions,
              executableEffects = allEffects.map(FollowModeNeutralizer::transformEffect),
              properties =
                  buildMap {
                    put(COST_PROPERTY, NumberValue(projectInfo?.cost ?: 0))
                    projectInfo?.requirement?.let {
                      put(REQUIREMENT_PROPERTY, RequirementValue(it))
                    }
                  },
              extraNodes =
                  setOfNotNull(sourceDeck?.className) +
                      componentClasses.map(ClassDeclaration::className) +
                      actionSelectors(sourceActions),
          )
        }
  }

  internal fun backedBy(klass: PetClass): CardDefinition {
    require(klass.className == className)
    return CardDefinition(data, klass)
  }

  private fun PetClass.representedClasses(): List<PetClass> =
      dependencies.typeDependencies().mapNotNull { dependency ->
        dependency.boundType.takeIf { it.rootClass.className == CLASS }?.representedClass
      }

  private fun PetClass.representedCardClass(): PetClass? {
    val cardBack = classTable.getClass(CARD_BACK)
    return representedClasses().singleOrNull { it.isSubtypeOf(cardBack) }
  }

  private fun PetClass.representedResourceClass(): PetClass? {
    val cardResource = classTable.getClass(CARD_RESOURCE)
    return representedClasses().singleOrNull { it.isSubtypeOf(cardResource) }
  }

  private fun PetClass.loadedTags(): Multiset<ClassName> {
    val tag = classTable.getClass(TAG)
    val names =
        declaration.authoredEffects
            .filter { it.automatic && it.trigger == WhenGain }
            .flatMap { it.instruction.descendantsOfType<Gain>() }
            .flatMap { gained ->
              val gainedClass = classTable.getClass(gained.gaining.className)
              if (gainedClass.isSubtypeOf(tag)) {
                List((gained.count as ActualScalar).value) { gainedClass.className }
              } else {
                emptyList()
              }
            }
    return HashMultiset.of(names)
  }

  private fun PetClass.loadedImmediate(): InstructionGroup? {
    val instructions =
        loadedNonActionEffects()
            .filter { !it.automatic && it.trigger == WhenGain }
            .map {
              it.instruction
            }
    return instructions
        .takeIf { it.isNotEmpty() }
        ?.let {
          InstructionGroup.of(InstructionGroup.createTree(it))
        }
  }

  private fun PetClass.loadedAuthoredEffects(): List<Effect> =
      loadedNonActionEffects().filterNot { effect ->
        effect.trigger == WhenGain && (!effect.automatic || effect.containsTagGain())
      }

  private fun PetClass.loadedNonActionEffects(): List<Effect> = declaration.authoredEffects

  private fun Effect.containsTagGain(): Boolean {
    val klass = requireNotNull(loadedClass)
    val tag = klass.classTable.getClass(TAG)
    return instruction.descendantsOfType<Gain>().any { gain ->
      klass.classTable.getClass(gain.gaining.className).isSubtypeOf(tag)
    }
  }

  /** The deck this card belongs to; see [CardDefinition.deck]. */
  public enum class Deck(public val className: ClassName) {
    PROJECT(PROJECT_CARD),
    PRELUDE(PRELUDE_CARD),
    CORPORATION(CORPORATION_CARD),
  }

  /** A kind (color) of project; see [CardDefinition.ProjectInfo.kind]. */
  internal enum class ProjectKind(internal val className: ClassName) {
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
      val tags: List<String> = emptyList(),
      val immediate: String? = null,
      val actions: List<String> = emptyList(),
      val effects: List<String> = emptyList(),
      val components: Set<String> = emptySet(),
      val requirement: String? = null,
      val cost: Int = 0,
      val projectKind: String? = null,
  ) {
    init {
      cn(name)
      require(replaces?.isNotEmpty() != false)
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

  private fun deriveResourceTypeCandidates(): Set<ClassName> {
    val cardNodes = listOfNotNull<PetNode>(sourceImmediate) + sourceActions + sourceEffects
    val authoredNodes = cardNodes + componentClasses.flatMap(ClassDeclaration::allNodes)
    fun isHeldByCard(expression: Expression): Boolean =
        expression.arguments == listOf(THIS.expression)

    val heldByThis =
        cardNodes
            .flatMap { it.descendantsOfType<Expression>() }
            .filter(::isHeldByCard)
            .mapTo(linkedSetOf(), Expression::className)

    val used = buildSet {
      sourceActions
          .mapNotNull(Action::cost)
          .flatMap { it.descendantsOfType<Action.Cost.Spend>() }
          .mapTo(this) { it.scaledEx.expression.className }
      authoredNodes
          .flatMap { it.descendantsOfType<Change>() }
          .mapNotNull(Change::removing)
          .mapTo(this, Expression::className)
      authoredNodes
          .flatMap { it.descendantsOfType<Count>() }
          .mapTo(this) { it.expression.className }
    }
    val candidates = (heldByThis intersect used).toMutableSet()

    val acceptsFromThis =
        sourceEffects
            .flatMap { it.instruction.descendantsOfType<Gain>() }
            .any {
              it.gaining.className == ACCEPT_FROM_CARD && isHeldByCard(it.gaining)
            }
    if (acceptsFromThis) {
      val stocked =
          (sourceActions.flatMap { it.instruction.descendantsOfType<Gain>() } +
                  sourceEffects.flatMap { it.instruction.descendantsOfType<Gain>() })
              .mapTo(linkedSetOf()) { it.gaining.className }
              .minus(ACCEPT_FROM_CARD)
      val explicitlyStocked = heldByThis intersect stocked
      candidates += explicitlyStocked.ifEmpty {
        sourceActions
            .flatMap { it.instruction.descendantsOfType<Gain>() }
            .mapTo(linkedSetOf()) { it.gaining.className }
      }
      require(candidates.isNotEmpty()) {
        "$className accepts payment from itself but does not identify its stocked resource"
      }
    }

    return candidates
  }

  private companion object {
    val ACCEPT_FROM_CARD = cn("AcceptFromCard")
    val CARD_BACK = cn("CardBack")
    val COST_PROPERTY = PropertyName("cost")
    val REQUIREMENT_PROPERTY = PropertyName("requirement")
  }
}
