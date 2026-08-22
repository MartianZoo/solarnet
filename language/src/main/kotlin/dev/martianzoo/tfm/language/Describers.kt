package dev.martianzoo.tfm.language

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE
import dev.martianzoo.types.Class
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.DependencySet.DependencyPath
import dev.martianzoo.types.Type

/** Looks up the English description supplied for each component Class. */
internal class Describers(private val descriptions: Map<Class, ComponentDescriber>) {
  private val classTable =
      requireNotNull(descriptions.keys.firstOrNull()?.classTable) {
        "English descriptions must include at least one Class"
      }
  private val classesByName = classTable.allClasses().associateBy { it.className }

  init {
    require(descriptions.keys.all { it.classTable === classTable })
    validateInheritedFacts()
  }

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
            ComponentDescriber::discardable,
            ComponentDescriber::cardResource,
            ComponentDescriber::cardResourceHolder,
            ComponentDescriber::metricLocation,
            ComponentDescriber::track,
            ComponentDescriber::placement,
            ComponentDescriber::placementSite,
            ComponentDescriber::placementBonus,
            ComponentDescriber::spatialRelation,
            ComponentDescriber::productionSelection,
            ComponentDescriber::requirement,
            ComponentDescriber::directChange,
            ComponentDescriber::draw,
            ComponentDescriber::purchase,
            ComponentDescriber::score,
            ComponentDescriber::deadEndSignal,
            ComponentDescriber::playTrigger,
            ComponentDescriber::playedCard,
            ComponentDescriber::playedTagPhrase,
            ComponentDescriber::presenceCondition,
            ComponentDescriber::usedActionTrigger,
            ComponentDescriber::actionNumber,
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

  internal fun directChangeSubclassDeclaration(className: ClassName): ClassDeclaration? {
    val componentClass = classesByName.getValue(className)
    if (componentClass.abstract) return null
    val superclass = componentClass.directSuperclasses.singleOrNull() ?: return null
    val superclassDescription = descriptions[superclass] ?: return null
    if (
        superclassDescription.directChange == null ||
            !superclassDescription.directChangeForSubclasses
    ) {
      return null
    }
    val declaration = componentClass.declaration
    val supertype = declaration.supertypes.singleOrNull()
    if (
        declaration.kind != ClassDeclaration.ClassKind.CONCRETE ||
            declaration.custom ||
            declaration.dependencies.isNotEmpty() ||
            supertype?.simple != true ||
            supertype.className != superclass.className ||
            declaration.invariants.isNotEmpty() ||
            declaration.defaultsDeclaration != ClassDeclaration.DefaultsDeclaration() ||
            declaration.properties.isNotEmpty()
    ) {
      return null
    }
    return declaration
  }

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
      componentNoun(className, count).takeIf { concrete(className) && isPlainGain(className) }

  internal fun plainGainCategoryNoun(className: ClassName, count: Int): String? =
      componentNoun(className, count).takeIf { isPlainGain(className) }

  internal fun isPlainGain(className: ClassName): Boolean {
    return isStandardResource(className)
  }

  internal fun isStandardResource(className: ClassName): Boolean =
      isSubtypeOf(className, STANDARD_RESOURCE)

  internal fun concreteStandardResources(): Set<ClassName> =
      classesByName.values
          .filter { !it.abstract && it.isSubtypeOf(classesByName.getValue(STANDARD_RESOURCE)) }
          .mapTo(linkedSetOf(), Class::className)

  internal fun isCardResource(className: ClassName): Boolean = isSubtypeOf(className, CARD_RESOURCE)

  private fun isTag(className: ClassName): Boolean = isSubtypeOf(className, TAG)

  internal fun isProduction(className: ClassName): Boolean = isSubtypeOf(className, PRODUCTION)

  internal fun isGameParticipant(className: ClassName): Boolean = isSubtypeOf(className, PLAYER)

  internal fun isGenerationScoped(className: ClassName): Boolean =
      isSubtypeOf(className, GENERATIONAL)

  internal fun isEndTrigger(className: ClassName): Boolean = isSubtypeOf(className, END)

  private fun isSubtypeOf(className: ClassName, superclassName: ClassName): Boolean =
      classesByName.getValue(className).isSubtypeOf(classesByName.getValue(superclassName))

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
    if (!concrete(className) || !isTag(className)) return null
    val ordinaryName = className.toString().removeSuffix("Tag").lowercase()
    val isPlanetTag = isSubtypeOf(className, PLANET_TAG)
    val name = if (isPlanetTag) ordinaryName.replaceFirstChar(Char::uppercaseChar) else ordinaryName
    return name to isPlanetTag
  }

  internal fun tagName(requirement: Requirement.Min): Pair<String, Boolean>? {
    val metric = requirement.metric as? Metric.Count ?: return null
    if (!metric.expression.simple) return null
    return tagName(metric.expression.className)
  }

  internal fun cardResourceNounPhrase(className: ClassName, count: Int): NounPhrase? {
    val style = fact(className, ComponentDescriber::cardResource) ?: return null
    if (!concrete(className)) {
      val noun =
          fact(className, ComponentDescriber::noun) as? ComponentDescriber.Noun.Counted
              ?: return null
      return NounPhrase(noun.singular, noun.plural, count)
    }
    val noun = unCamelCase(className.toString())
    return when (style) {
      ComponentDescriber.CardResource.ORDINARY -> NounPhrase(noun, "${noun}s", count)
      ComponentDescriber.CardResource.SUFFIXED ->
          NounPhrase("$noun resource", "$noun resources", count)
    }
  }

  internal fun productionExpression(
      expression: Expression,
  ): Pair<List<Expression>, ClassName>? =
      parseProductionExpression(expression)?.takeIf { (_, resource) ->
        plainGainNoun(resource, 1) != null
      }

  internal fun productionCategoryExpression(
      expression: Expression,
  ): Pair<List<Expression>, ClassName>? =
      parseProductionExpression(expression)?.takeIf { (_, resource) ->
        plainGainCategoryNoun(resource, 1) != null
      }

  private fun parseProductionExpression(
      expression: Expression,
  ): Pair<List<Expression>, ClassName>? {
    val resolved =
        resolveExpression(expression) ?: return parseContextualProductionExpression(expression)
    val type = resolved.type
    if (!type.rootClass.isSubtypeOf(classesByName.getValue(PRODUCTION))) return null
    val resource =
        resolved.dependency(Key(PRODUCTION, 0))?.representedType()?.takeIf { it.refinement == null }
            ?: return null
    val ownerKey = Key(OWNED, 0)
    val owner = resolved.dependency(ownerKey) ?: return null
    val owners = if (owner.expression == ownerExpression) emptyList() else listOf(owner.expression)
    return owners to resource.className
  }

  // TODO: Resolve contextual This through linked type sources, then delete this positional
  // fallback.
  private fun parseContextualProductionExpression(
      expression: Expression,
  ): Pair<List<Expression>, ClassName>? {
    if (
        !isProduction(expression.className) ||
            expression.refinement != null ||
            expression.complement
    ) {
      return null
    }
    val resourceDependency = expression.arguments.lastOrNull() ?: return null
    if (
        resourceDependency.className != CLASS ||
            resourceDependency.arguments.size != 1 ||
            resourceDependency.refinement != null ||
            resourceDependency.complement
    ) {
      return null
    }
    val resource = resourceDependency.arguments.single()
    if (!resource.simple) return null
    return expression.arguments.dropLast(1) to resource.className
  }

  internal fun representedClass(expression: Expression): Expression? {
    val classType = representedClassType(expression) ?: return null
    val represented = classType.representedClass ?: return null
    if (classType.refinement != null) return null
    return represented.className.expression
  }

  internal fun representedExpression(expression: Expression): Expression? {
    val classType = representedClassType(expression) ?: return null
    return classType.representedExpression()
  }

  private fun representedClassType(expression: Expression): Type? {
    val resolved = resolveExpression(expression) ?: return null
    val key =
        resolved.type.rootClass.dependencies.keys.singleOrNull {
          resolved.selectedDependency(it)?.rootClass?.className == CLASS
        } ?: return null
    return resolved.dependency(key)
  }

  internal fun representedClassArgument(classExpression: Expression): Expression? {
    return resolveExpression(classExpression)?.type?.representedExpression()
  }

  internal fun resolveExpression(expression: Expression): ResolvedExpression? {
    if (expression.complement) return null
    val sourceType =
        try {
          classTable.resolve(expression)
        } catch (_: ExpressionException) {
          return null
        }
    val rootClass = sourceType.rootClass
    val sourceDependencies =
        rootClass.matchDependencyKeys(expression.arguments).zip(expression.arguments).toMap()
    val defaultArguments = rootClass.defaultType.expressionFull.arguments
    val defaults = rootClass.matchDependencyKeys(defaultArguments).zip(defaultArguments).toMap()
    val defaultedKeys =
        rootClass.dependencies.keys.filterTo(linkedSetOf()) { key ->
          val path = DependencyPath(listOf(key))
          rootClass.defaultType.dependencies.at(path) != rootClass.baseType.dependencies.at(path)
        }
    val semanticArguments =
        rootClass.dependencies.keys.map { sourceDependencies[it] ?: defaults.getValue(it) }
    val semanticExpression = expression.copy(arguments = semanticArguments)
    val semanticType =
        try {
          classTable.resolve(semanticExpression)
        } catch (_: ExpressionException) {
          return null
        }
    return ResolvedExpression(
        semanticType,
        sourceDependencies.keys + defaultedKeys,
        sourceDependencies,
    )
  }

  private fun Type.representedType(): Type? {
    return representedClass?.baseType
  }

  private fun Type.representedExpression(): Expression? {
    val represented = representedType() ?: return null
    return represented.expression.copy(refinement = refinement)
  }

  internal fun distinctOwnedKinds(expression: Expression): ComponentDescriber.Noun.Counted? {
    if (expression.className != CLASS || expression.arguments.size != 1 || expression.complement) {
      return null
    }
    val kind = expression.arguments.single()
    if (!kind.simple) return null
    val refinement = expression.refinement ?: return null
    if (refinement.forgiving) return null
    val minimum = refinement.requirement as? Requirement.Min ?: return null
    if (minimum.target != 1) return null
    val member = (minimum.metric as? Metric.Count)?.expression ?: return null
    if (
        member.className != kind.className ||
            member.arguments != listOf(ownerExpression) ||
            member.refinement != null ||
            member.complement
    ) {
      return null
    }
    return fact(kind.className, ComponentDescriber::distinctKinds)
  }

  internal fun concrete(className: ClassName): Boolean {
    val componentClass = classesByName[className] ?: return false
    return !componentClass.abstract
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

  internal val anyoneExpression = cn("Anyone").expression
  internal val notOwnerExpression = cn("Owner").expression.copy(complement = true)
  internal val ownerExpression = cn("Owner").expression
  internal val playerExpression = cn("Player").expression
  internal val thisExpression = cn("This").expression

  private companion object {
    val CARD_RESOURCE = cn("CardResource")
    val END = cn("End")
    val GENERATIONAL = cn("Generational")
    val PLANET_TAG = cn("PlanetTag")
    val PLAYER = cn("Player")
    val TAG = cn("Tag")
  }
}
