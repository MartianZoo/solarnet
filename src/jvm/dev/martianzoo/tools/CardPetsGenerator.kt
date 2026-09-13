package dev.martianzoo.tools

import dev.martianzoo.pets.DerivedClassLowerer
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.Transforming.immediateToEffect
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
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
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.tfm.carddata.CardData
import dev.martianzoo.tfm.carddata.CardDefinition

internal object CardPetsGenerator {
  internal fun renderBundle(bundleName: String): String {
    val cards = CardData.definitions(bundleName).map(::GeneratedCard)
    val renderedSupport = mutableSetOf<ClassDeclaration>()
    return buildString {
      appendLine(GENERATED_HEADER)
      cards.forEachIndexed { index, card ->
        if (index > 0) appendLine()
        appendLine(card.render())
        card.supportingDeclarations.filter(renderedSupport::add).forEach { declaration ->
          appendLine()
          appendLine(declaration)
        }
      }
    }
        .trimEnd() + '\n'
  }

  private class GeneratedCard(private val data: CardDefinition) {
    private val className = cn(data.name)
    private val derivedClasses = DerivedClassLowerer(className)

    private inline fun <reified P : PetNode> parseOwned(source: String): Parsed<P> {
      val node = Parsing.parse(P::class, source, derivedClasses)
      return Parsed(source, node)
    }

    private val deck = data.deck?.let(::cn)
    private val projectKind = data.projectKind?.let(::cn)
    private val immediate = data.immediate?.let { parseOwned<InstructionTree>(it) }
    private val actions = data.actions.map { parseOwned<Action>(it) }
    private val effects = data.effects.map { parseOwned<Effect>(it) }
    private val authoredAutomaticThisEffects = effects.filter {
      it.node.automatic && it.node.trigger == Effect.Trigger.WhenGain
    }
    private val authoredThisEffects = effects.filter {
      !it.node.automatic && it.node.trigger == Effect.Trigger.WhenGain
    }
    private val otherEffects = effects.filter { it.node.trigger != Effect.Trigger.WhenGain }
    private val generatedTagEffect =
        immediateToEffect(
            InstructionGroup.createTree(
                (data.tags + listOfNotNull("EventTag".takeIf { projectKind == EVENT_CARD }))
                    .groupingBy(::cn)
                    .eachCount()
                    .map { (tag, count) -> gain(tag.of(THIS), count, quantifier = null) }
            ),
            true,
        )
    private val componentClasses = data.components.map(::parseOneLinerClass)
    private val invariants = data.invariants.map { parseOwned<Requirement>(it) }
    private val requirement: Requirement? =
        data.requirement?.let { parseOwned<Requirement>(it).node }
    private val autoSelectWhen: Requirement? =
        data.autoSelectWhen?.let { parseOwned<Requirement>(it).node }
    private val resourceType = deriveResourceTypeCandidates().singleOrNull()

    internal val declaration: ClassDeclaration by lazy {
      val onPlayEffects =
          listOfNotNull(immediate).mapNotNull { parsed ->
            immediateToEffect(InstructionGroup.of(parsed.node), false)
          }
      val cardBack = (deck ?: CARD_BACK).classExpression()
      val roles =
          listOfNotNull(
              projectKind?.expression,
              resourceType?.let { RESOURCE_CARD.of(it.classExpression()) },
              ACTION_CARD.expression.takeIf { actions.isNotEmpty() },
          )
      val supertypes =
          if (projectKind != null) {
            roles.toSet()
          } else if (roles.isEmpty()) {
            setOf(CARD_FRONT.of(cardBack))
          } else {
            buildSet {
              add(roles.first().appendArguments(listOf(cardBack)))
              addAll(roles.drop(1))
            }
          }
      ClassDeclaration(
          className = className,
          kind = CONCRETE,
          supertypes = supertypes,
          invariants = invariants.mapTo(linkedSetOf(), Parsed<Requirement>::node),
          authoredEffects =
              authoredAutomaticThisEffects.map(Parsed<Effect>::node) +
                  listOfNotNull(generatedTagEffect) +
                  onPlayEffects +
                  authoredThisEffects.map(Parsed<Effect>::node) +
                  otherEffects.map(Parsed<Effect>::node),
          authoredActions = actions.map(Parsed<Action>::node),
          properties =
              buildMap {
                put(COST_PROPERTY, NumberValue(data.cost))
                requirement?.let { put(REQUIREMENT_PROPERTY, RequirementValue(it)) }
                autoSelectWhen?.let { put(AUTO_SELECT_WHEN_PROPERTY, RequirementValue(it)) }
              },
      )
    }

    internal fun render(): String = buildString {
      append("CLASS ${declaration.className}")
      declaration.supertypes.sortedBy(Expression::toString).joinTo(this, ", ", " : ")
      appendLine(" {")
      declaration.properties.forEach { (name, value) -> appendLine("  $name = $value") }
      invariants.forEach { appendLine("  HAS ${it.render()}") }
      val renderedEffects = buildList {
        authoredAutomaticThisEffects.mapTo(this) { it.render() }
        generatedTagEffect?.let { add(it.toString()) }
        immediate?.let { add("This: ${it.render()}") }
        authoredThisEffects.mapTo(this) { it.render() }
        otherEffects.mapTo(this) { it.render() }
      }
      if (renderedEffects.isNotEmpty() || actions.isNotEmpty()) appendLine()
      renderedEffects.forEach { appendLine("  $it") }
      if (renderedEffects.isNotEmpty() && actions.isNotEmpty()) appendLine()
      actions.forEach { appendLine("  ${it.render()}") }
      append('}')
    }

    internal val supportingDeclarations: List<ClassDeclaration>
      get() = componentClasses

    /** A card holds a resource exactly when its own instructions can meaningfully use it. */
    private fun deriveResourceTypeCandidates(): Set<ClassName> {
      val cardNodes =
          listOfNotNull<PetNode>(immediate?.node) +
              actions.map(Parsed<Action>::node) +
              effects.map(Parsed<Effect>::node)
      val authoredNodes = cardNodes + componentClasses.flatMap(ClassDeclaration::allNodes)
      fun isHeldByCard(expression: Expression): Boolean =
          expression.arguments == listOf(THIS.expression)

      val heldByThis =
          cardNodes
              .flatMap { it.descendantsOfType<Expression>() }
              .filter(::isHeldByCard)
              .mapTo(linkedSetOf(), Expression::className)
      val used = buildSet {
        actions
            .map(Parsed<Action>::node)
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
      componentClasses
          .filter { declaration ->
            declaration.supertypes.none { it.className == CARD_RESOURCE }
          }
          .mapTo(mutableSetOf(), ClassDeclaration::className)
          .let(candidates::removeAll)
      val acceptsFromThis =
          effects
              .map(Parsed<Effect>::node)
              .flatMap { it.instruction.descendantsOfType<Gain>() }
              .any { it.gaining.className == ACCEPTING_FROM_CARD && isHeldByCard(it.gaining) }
      if (acceptsFromThis) {
        val stocked =
            (actions.map(Parsed<Action>::node).flatMap {
                  it.instruction.descendantsOfType<Gain>()
                } +
                    effects.map(Parsed<Effect>::node).flatMap {
                      it.instruction.descendantsOfType<Gain>()
                    })
                .mapTo(linkedSetOf()) { it.gaining.className }
                .minus(ACCEPTING_FROM_CARD)
        candidates +=
            (heldByThis intersect stocked).ifEmpty {
              actions
                  .map(Parsed<Action>::node)
                  .flatMap { it.instruction.descendantsOfType<Gain>() }
                  .mapTo(linkedSetOf()) { it.gaining.className }
            }
        require(candidates.isNotEmpty()) {
          "$className accepts payment from itself but does not identify its stocked resource"
        }
      }
      return candidates
    }

    private data class Parsed<P : PetNode>(val source: String, val node: P) {
      fun render(): String = source
    }
  }

  private val CARD_BACK = cn("CardBack")
  private val CARD_FRONT = cn("CardFront")
  private val RESOURCE_CARD = cn("ResourceCard")
  private val CARD_RESOURCE = cn("CardResource")
  private val ACTION_CARD = cn("ActionCard")
  private val EVENT_CARD = cn("EventCard")
  private val ACCEPTING_FROM_CARD = cn("AcceptingFromCard")
  private val COST_PROPERTY = PropertyName("cost")
  private val REQUIREMENT_PROPERTY = PropertyName("requirement")
  private val AUTO_SELECT_WHEN_PROPERTY = PropertyName("autoSelectWhen")
  private const val GENERATED_HEADER =
      "// Generated from Terraforming Mars card data by :tools:generateCardPets. Do not edit."
}
