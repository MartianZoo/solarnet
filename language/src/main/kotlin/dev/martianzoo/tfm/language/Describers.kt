package dev.martianzoo.tfm.language

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.Class
import dev.martianzoo.types.Dependency.Key

/** Looks up the English description supplied for each component Class. */
internal class Describers(
    private val descriptions: Map<Class, ComponentDescriber>,
    private val sourceDeclarations: Map<ClassName, ClassDeclaration> = emptyMap(),
) {
  internal val expressions = ExpressionResolver(descriptions.keys)
  private val classesByName = expressions.classesByName

  init {
    if (sourceDeclarations.isEmpty()) validateInheritedFacts()
  }

  internal fun withSourceDeclarations(declarations: List<ClassDeclaration>): Describers =
      if (declarations.isEmpty()) this
      else Describers(descriptions, declarations.associateBy(ClassDeclaration::className))

  internal fun sourceDeclaration(className: ClassName): ClassDeclaration =
      sourceDeclarations[className] ?: classesByName.getValue(className).declaration

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
        descriptions[superclass]?.let(fact)?.let { superclass to it }
      }

  private fun validateInheritedFacts() {
    val facts: List<(ComponentDescriber) -> Any?> =
        listOf(
            ComponentDescriber::noun,
            ComponentDescriber::changeFrame,
            ComponentDescriber::cardResourceHolder,
            ComponentDescriber::metricLocation,
            ComponentDescriber::placementSite,
            ComponentDescriber::placementBonus,
            ComponentDescriber::spatialRelation,
            ComponentDescriber::productionSelection,
            ComponentDescriber::requirement,
            ComponentDescriber::purchase,
            ComponentDescriber::score,
            ComponentDescriber::deadEndSignal,
            ComponentDescriber::playTrigger,
            ComponentDescriber::playedCard,
            ComponentDescriber::playedTagPhrase,
            ComponentDescriber::presenceCondition,
            ComponentDescriber::usedActionTrigger,
            ComponentDescriber::actionUse,
            ComponentDescriber::spentResourceTrigger,
            ComponentDescriber::paymentRole,
            ComponentDescriber::implicitPaymentResource,
            ComponentDescriber::requirementShortfall,
            ComponentDescriber::requirementKind,
            ComponentDescriber::distinctKinds,
            ComponentDescriber::countNoun,
            ComponentDescriber::metricCount,
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
    val direct = descriptions[classesByName.getValue(className)]?.placementSite
    return site.takeIf { direct != null }
  }

  internal fun changeFrame(className: ClassName): ComponentDescriber.ChangeFrame? =
      fact(className, ComponentDescriber::changeFrame)

  internal fun positionedFrame(className: ClassName): ComponentDescriber.ChangeFrame.Positioned? =
      changeFrame(className) as? ComponentDescriber.ChangeFrame.Positioned

  internal fun scaleFrame(className: ClassName): ComponentDescriber.ChangeFrame.Scale? =
      changeFrame(className) as? ComponentDescriber.ChangeFrame.Scale

  internal fun resolveExpression(expression: Expression): ResolvedExpression? =
      expressions.resolve(expression)

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

  internal fun isCardResource(className: ClassName): Boolean = expressions.isCardResource(className)

  internal fun isProduction(className: ClassName): Boolean = expressions.isProduction(className)

  internal fun isPlayerOwned(className: ClassName): Boolean = expressions.isPlayerOwned(className)

  internal fun isGameParticipant(className: ClassName): Boolean =
      expressions.isGameParticipant(className)

  internal fun isGenerationScoped(className: ClassName): Boolean =
      expressions.isGenerationScoped(className)

  internal fun isEndTrigger(className: ClassName): Boolean = expressions.isEndTrigger(className)

  internal val anyoneExpression: Expression = expressions.anyoneExpression
  internal val notOwnerExpression: Expression = expressions.notOwnerExpression
  internal val ownerExpression: Expression = expressions.ownerExpression
  internal val playerExpression: Expression = expressions.playerExpression
  internal val thisExpression: Expression = expressions.thisExpression

  internal fun componentNoun(className: ClassName, count: Int): String =
      describedNoun(className, fact(className, ComponentDescriber::noun), count)

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

  internal fun componentNounPhrase(className: ClassName, count: Int): NounPhrase {
    val noun = fact(className, ComponentDescriber::noun)
    return when (noun) {
      is ComponentDescriber.Noun.Counted -> NounPhrase(noun.singular, noun.plural, count)
      is ComponentDescriber.Noun.Fixed -> NounPhrase(noun.text, noun.text, count)
      ComponentDescriber.Noun.ClassName,
      null -> NounPhrase(unCamelCase(className.toString()), count = count)
    }
  }

  internal fun cardResourceNoun(className: ClassName, count: Int): String? {
    return cardResourceNounPhrase(className, count)?.noun()
  }

  internal fun tagName(className: ClassName): Pair<String, Boolean>? {
    if (!expressions.concrete(className) || !expressions.isTag(className)) return null
    val ordinaryName = className.toString().removeSuffix("Tag").lowercase()
    val isPlanetTag = expressions.isPlanetTag(className)
    val name = if (isPlanetTag) ordinaryName.replaceFirstChar(Char::uppercaseChar) else ordinaryName
    return name to isPlanetTag
  }

  internal fun tagName(requirement: Requirement.Min): Pair<String, Boolean>? {
    val metric = requirement.metric as? Metric.Count ?: return null
    if (!metric.expression.simple) return null
    return tagName(metric.expression.className)
  }

  internal fun cardResourceNounPhrase(className: ClassName, count: Int): NounPhrase? {
    if (changeFrame(className) != ComponentDescriber.ChangeFrame.Held) return null
    val noun =
        fact(className, ComponentDescriber::noun) as? ComponentDescriber.Noun.Counted ?: return null
    return NounPhrase(noun.singular, noun.plural, count)
  }

  internal fun indefiniteArticle(noun: String): String =
      if (noun.first().lowercaseChar() in "aeiou") "an" else "a"

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
}
