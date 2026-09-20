package dev.martianzoo.tfm.text

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.types.inferTypeVariables
import dev.martianzoo.tfm.canon.TfmClasses.PROD

/** Looks up the English description supplied for each component Class. */
internal class Describers
private constructor(
    private val classTable: ClassTable,
    private val descriptions: Map<ClassName, ComponentDescriber>,
    private val cardContext: CardContext?,
) {
  internal constructor(
      classTable: ClassTable,
      descriptions: Map<ClassName, ComponentDescriber>,
  ) : this(classTable, descriptions, null)

  internal val expressions = ExpressionResolver(classTable)
  private val classesByName = expressions.classesByName

  init {
    val unknownDescriptions = descriptions.keys - classesByName.keys
    require(unknownDescriptions.isEmpty()) {
      "English descriptions name Classes outside the supplied table: $unknownDescriptions"
    }
    validateInheritedFacts()
  }

  internal fun declaration(className: ClassName) = classesByName.getValue(className).declaration

  internal fun forCard(resourceType: ClassName?): Describers =
      Describers(classTable, descriptions, CardContext(resourceType))

  internal fun unboundCardResourceDestination(resourceType: ClassName): Determiner =
      if (
          cardContext == null ||
              cardContext.resourceType?.let { expressions.isSubtypeOf(it, resourceType) } == true
      ) {
        Determiner.ANY
      } else {
        Determiner.ANOTHER
      }

  internal fun lowerProductionSyntax(instructionTree: InstructionTree): InstructionTree =
      productionSyntaxLowerer().transformInstructionTree(instructionTree)

  internal fun prepareForRendering(instructionTree: InstructionTree): InstructionTree =
      classTable
          .inferTypeVariables()
          .transformInstructionTree(lowerProductionSyntax(instructionTree))

  internal fun lowerProductionSyntax(action: Action): Action =
      productionSyntaxLowerer().transformAction(action)

  internal fun prepareForRendering(action: Action): Action =
      classTable.inferTypeVariables().transformAction(lowerProductionSyntax(action))

  internal fun lowerProductionSyntax(effect: Effect): Effect =
      productionSyntaxLowerer().transformEffect(effect)

  internal fun prepareForRendering(effect: Effect): Effect =
      classTable.inferTypeVariables().transformEffect(lowerProductionSyntax(effect))

  internal fun lowerProductionSyntax(requirement: Requirement): Requirement =
      productionSyntaxLowerer().transformRequirement(requirement)

  internal fun lowerProductionSyntax(metric: Metric): Metric =
      productionSyntaxLowerer().transformMetric(metric)

  private fun productionSyntaxLowerer(): PetTransformer =
      classTable.transformDispatcher(setOf(PROD))

  internal fun gainDefaultExpressions(className: ClassName): List<Expression> =
      classesByName.getValue(className).defaults.gainOnly.dependencies.typeDependencies().map {
        it.boundType.expressionFull
      }

  internal fun resolvedRemovalModality(removal: Remove): Modality =
      removal.quantifier?.modality()
          ?: classesByName
              .getValue(removal.removing.className)
              .defaults
              .removeOnly
              .quantifier
              .modality()

  internal fun <T> fact(
      className: ClassName,
      fact: (ComponentDescriber) -> T?,
  ): T? {
    val componentClass = classesByName.getValue(className)
    val providers = providers(componentClass, fact)
    val nearest = providers.filter { (provider) ->
      providers.none { (other) -> other !== provider && other.isSubtypeOf(provider) }
    }
    return nearest.map { (_, value) -> value }.distinct().singleOrNull()
  }

  private fun <T> providers(
      componentClass: Class,
      fact: (ComponentDescriber) -> T?,
  ): List<Pair<Class, T>> =
      componentClass.allSuperclasses().mapNotNull { superclass ->
        descriptions[superclass.className]?.let(fact)?.let { superclass to it }
      }

  private fun validateInheritedFacts() {
    val facts: List<(ComponentDescriber) -> Any?> =
        listOf(
            ComponentDescriber::noun,
            ComponentDescriber::numericSingularChange,
            ComponentDescriber::changeFrame,
            ComponentDescriber::cardResourceHolder,
            ComponentDescriber::metricLocation,
            ComponentDescriber::placementSite,
            ComponentDescriber::placementBonus,
            ComponentDescriber::spatialRelation,
            ComponentDescriber::productionOffset,
            ComponentDescriber::requirement,
            ComponentDescriber::requirementCondition,
            ComponentDescriber::score,
            ComponentDescriber::deadEndSignal,
            ComponentDescriber::triggerFrame,
            ComponentDescriber::presenceCondition,
            ComponentDescriber::actionUse,
            ComponentDescriber::paymentRole,
            ComponentDescriber::basePaymentValue,
            ComponentDescriber::implicitPaymentResource,
            ComponentDescriber::requirementShortfall,
            ComponentDescriber::requirementKind,
            ComponentDescriber::capitalizeTagName,
            ComponentDescriber::distinctKinds,
            ComponentDescriber::countNoun,
            ComponentDescriber::metricCount,
            ComponentDescriber::printedIconCount,
        )
    classesByName.values.forEach { componentClass ->
      facts.forEach { fact ->
        val providers = providers(componentClass, fact)
        val nearest = providers.filter { (provider) ->
          providers.none { (other) -> other !== provider && other.isSubtypeOf(provider) }
        }
        check(nearest.map { (_, value) -> value }.distinct().size <= 1) {
          "${componentClass.className} inherits conflicting English component knowledge from " +
              nearest.joinToString { (provider) -> provider.className.toString() }
        }
      }
    }
  }

  internal fun placementSite(className: ClassName): ComponentDescriber.PlacementSite? {
    val site = fact(className, ComponentDescriber::placementSite) ?: return null
    if (site.forSubclasses) return site
    val direct = descriptions[className]?.placementSite
    return site.takeIf { direct != null }
  }

  internal fun metricCount(className: ClassName): ComponentDescriber.MetricCount? {
    val count = fact(className, ComponentDescriber::metricCount) ?: return null
    if (count.forSubclasses) return count
    return count.takeIf { descriptions[className]?.metricCount != null }
  }

  internal fun changeFrame(className: ClassName): ComponentDescriber.ChangeFrame? =
      fact(className, ComponentDescriber::changeFrame)

  internal fun triggerFrame(className: ClassName): ComponentDescriber.TriggerFrame? =
      fact(className, ComponentDescriber::triggerFrame)

  internal fun positionedFrame(className: ClassName): ComponentDescriber.ChangeFrame.Positioned? =
      changeFrame(className) as? ComponentDescriber.ChangeFrame.Positioned

  internal fun scaleFrame(className: ClassName): ComponentDescriber.ChangeFrame.Scale? =
      changeFrame(className) as? ComponentDescriber.ChangeFrame.Scale

  internal fun resolveExpression(expression: Expression): ResolvedExpression? =
      expressions.resolve(expression)

  internal fun allConcreteSubtypes(type: Type) = classTable.allConcreteSubtypes(type)

  internal fun resolveExpression(
      expression: Expression,
      contextualThisKey: Key,
  ): ResolvedExpression? = expressions.resolve(expression, contextualThisKey)

  internal fun representedClass(expression: Expression): Expression? =
      expressions.representedClass(expression)

  internal fun representedExpression(expression: Expression): Expression? =
      expressions.representedExpression(expression)

  internal fun representedClassArgument(expression: Expression): Expression? =
      expressions.representedClassArgument(expression)

  internal fun concrete(className: ClassName): Boolean = expressions.concrete(className)

  internal fun isStandardResource(className: ClassName): Boolean =
      expressions.isStandardResource(className)

  internal fun hasBasePaymentValue(className: ClassName): Boolean =
      fact(className, ComponentDescriber::basePaymentValue) == true

  internal fun isCardResource(className: ClassName): Boolean = expressions.isCardResource(className)

  internal fun isTag(className: ClassName): Boolean = expressions.isTag(className)

  internal fun isProduction(className: ClassName): Boolean = expressions.isProduction(className)

  internal fun isProductionOffset(expression: Expression): Boolean =
      expression.refinement == null &&
          fact(expression.className, ComponentDescriber::productionOffset) == true

  internal fun isPlayerOwned(className: ClassName): Boolean = expressions.isPlayerOwned(className)

  internal fun isGameParticipant(className: ClassName): Boolean =
      expressions.isGameParticipant(className)

  internal fun isNotOwner(expression: Expression): Boolean = expressions.isNotOwner(expression)

  internal fun isGenerationScoped(className: ClassName): Boolean =
      expressions.isGenerationScoped(className)

  internal fun isEndTrigger(className: ClassName): Boolean = expressions.isEndTrigger(className)

  internal val anyoneExpression: Expression = expressions.anyoneExpression
  internal val ownerExpression: Expression = expressions.ownerExpression
  internal val playerExpression: Expression = expressions.playerExpression
  internal val thisExpression: Expression = expressions.thisExpression

  internal fun componentNoun(className: ClassName, count: Int): String =
      describedNoun(className, fact(className, ComponentDescriber::noun), count)

  private fun usesNumericSingularChange(className: ClassName): Boolean =
      fact(className, ComponentDescriber::numericSingularChange) == true

  internal fun describedNoun(
      className: ClassName,
      noun: ComponentDescriber.Noun?,
      count: Int,
  ): String =
      when (noun) {
        is ComponentDescriber.Noun.Counted -> if (count == 1) noun.singular else noun.plural
        is ComponentDescriber.Noun.Fixed -> noun.text
        ComponentDescriber.Noun.ClassName,
        null -> unCamelCase(className.toString())
      }

  internal fun plainGainNoun(className: ClassName, count: Int): String? =
      componentNoun(className, count).takeIf {
        expressions.concrete(className) && expressions.isStandardResource(className)
      }

  internal fun plainGainCategoryNoun(className: ClassName, count: Int): String? =
      componentNoun(className, count).takeIf { expressions.isStandardResource(className) }

  internal fun resolveCardResource(expression: Expression): ResolvedExpression? {
    return expressions.resolveCardResource(expression)
  }

  internal fun cardResourceHolder(resolved: ResolvedExpression): Expression? =
      expressions.cardResourceHolder(resolved)

  internal fun cardResourceHasHolder(
      resolved: ResolvedExpression,
      holder: Expression,
  ): Boolean {
    return expressions.cardResourceHasHolder(resolved, holder)
  }

  internal fun resolveHeldResource(expression: Expression): ResolvedExpression? =
      expressions.resolveHeldResource(expression)

  internal fun heldResourceHolder(resolved: ResolvedExpression): Expression? =
      expressions.heldResourceHolder(resolved)

  internal fun heldResourceHasHolder(
      resolved: ResolvedExpression,
      holder: Expression,
  ): Boolean = expressions.heldResourceHasHolder(resolved, holder)

  internal fun componentNounPhrase(className: ClassName, count: Int): NounPhrase {
    val noun = fact(className, ComponentDescriber::noun)
    return when (noun) {
      is ComponentDescriber.Noun.Counted -> NounPhrase(noun.singular, noun.plural, count)
      is ComponentDescriber.Noun.Fixed -> NounPhrase(noun.text, noun.text, count)
      ComponentDescriber.Noun.ClassName,
      null -> NounPhrase(unCamelCase(className.toString()), count = count)
    }
  }

  internal fun quantifiedComponentNounPhrase(
      className: ClassName,
      count: Int,
      singular: String = componentNoun(className, 1),
      plural: String = componentNoun(className, 2),
      determiner: Determiner = Determiner.INDEFINITE,
  ): NounPhrase =
      if (count != 1 || usesNumericSingularChange(className)) {
        NounPhrase(singular, plural, count = count)
      } else {
        NounPhrase(singular, plural, determiner = determiner)
      }

  internal fun cardResourceNoun(className: ClassName, count: Int): String? {
    return cardResourceNounPhrase(className, count)?.noun()
  }

  internal fun tagName(className: ClassName): String? {
    if (!expressions.concrete(className) || !expressions.isTag(className)) return null
    val ordinaryName = className.toString().removeSuffix("Tag").lowercase()
    return if (fact(className, ComponentDescriber::capitalizeTagName) == true) {
      ordinaryName.replaceFirstChar(Char::uppercaseChar)
    } else {
      ordinaryName
    }
  }

  internal fun tagName(requirement: Requirement.Min): String? {
    val expression = countedExpression(requirement) ?: return null
    if (!expression.simple) return null
    return tagName(expression.className)
  }

  internal fun playedTagPhrase(className: ClassName): NounPhrase? {
    tagName(className)?.let { name ->
      return NounPhrase("$name tag", determiner = Determiner.INDEFINITE)
    }
    return (triggerFrame(className) as? ComponentDescriber.TriggerFrame.PlayTag)?.noun?.let {
      NounPhrase(it, determiner = Determiner.INDEFINITE)
    }
  }

  internal fun cardResourceNounPhrase(className: ClassName, count: Int): NounPhrase? {
    if (changeFrame(className) != ComponentDescriber.ChangeFrame.Held) return null
    val noun =
        fact(className, ComponentDescriber::noun) as? ComponentDescriber.Noun.Counted ?: return null
    return NounPhrase(noun.singular, noun.plural, count)
  }

  private fun unCamelCase(name: String): String = buildString {
    name.forEachIndexed { index, character ->
      val previous = name.getOrNull(index - 1)
      val next = name.getOrNull(index + 1)
      if (character == '_') {
        append(' ')
      } else {
        val startsWord =
            previous != null &&
                character.isUpperCase() &&
                (previous.isLowerCase() ||
                    previous.isDigit() ||
                    (previous.isUpperCase() && next?.isLowerCase() == true))
        if (startsWord) append(' ')
        append(character.lowercaseChar())
      }
    }
  }

  private data class CardContext(val resourceType: ClassName?)
}
