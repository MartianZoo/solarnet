package dev.martianzoo.tfm.language

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.types.Class

/** Looks up the English description supplied for each component Class. */
internal class Describers(private val descriptions: Map<Class, ComponentDescriber>) {
  private val classesByName = descriptions.keys.associateBy { it.className }

  internal fun <T> fact(
      className: ClassName,
      fact: (ComponentDescriber) -> T?,
  ): T? {
    val componentClass = classesByName.getValue(className)
    val providers =
        componentClass.allSuperclasses().mapNotNull { superclass ->
          descriptions.getValue(superclass).let(fact)?.let { superclass to it }
        }
    val nearest = providers.filter { (provider) ->
      providers.none { (other) -> other !== provider && other.isSubtypeOf(provider) }
    }
    val values = nearest.map { (_, value) -> value }.distinct()
    check(values.size <= 1) {
      "${componentClass.className} inherits conflicting English component knowledge from " +
          nearest.joinToString { (provider) -> provider.className.toString() }
    }
    return values.singleOrNull()
  }

  internal fun hasBehaviorBearingExtraClass(card: CardDefinition): Boolean =
      card.extraClasses.any { !isTextNeutralExtraClass(it) && !directlyDescribesGain(it, card) }

  private fun directlyDescribesGain(
      declaration: ClassDeclaration,
      card: CardDefinition,
  ): Boolean {
    val componentClass = classesByName.getValue(declaration.className)
    val ownDescription = descriptions.getValue(componentClass)
    val described =
        ownDescription.directChange != null ||
            (!componentClass.abstract &&
                componentClass.directSuperclasses.singleOrNull()?.let { superclass ->
                  val superclassDescription = descriptions.getValue(superclass)
                  superclassDescription.directChange != null &&
                      superclassDescription.directChangeForSubclasses
                } == true)
    if (!described) return false
    return card.immediate?.descendantsOfType<Gain>()?.any {
      it.gaining.className == declaration.className
    } == true
  }

  internal fun declaration(className: ClassName): ClassDeclaration =
      classesByName.getValue(className).declaration

  private fun isTextNeutralExtraClass(declaration: ClassDeclaration): Boolean {
    val superclass = declaration.supertypes.singleOrNull() ?: return false
    if (
        declaration.kind != ClassDeclaration.ClassKind.CONCRETE ||
            declaration.custom ||
            declaration.dependencies.isNotEmpty() ||
            declaration.invariants.isNotEmpty() ||
            declaration.effects.isNotEmpty() ||
            declaration.defaultsDeclaration != ClassDeclaration.DefaultsDeclaration() ||
            declaration.properties.isNotEmpty() ||
            superclass.refinement != null ||
            superclass.complement
    ) {
      return false
    }
    val superclassDescription = descriptions.getValue(classesByName.getValue(superclass.className))
    return superclassDescription.textNeutralSubclasses
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
    return fact(className, ComponentDescriber::standardResource) == true
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
    if (!concrete(className)) return null
    val style = fact(className, ComponentDescriber::tag) ?: return null
    val ordinaryName = className.toString().removeSuffix("Tag").lowercase()
    val isPlanetTag = style == ComponentDescriber.Tag.PLANET
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
    if (
        fact(expression.className, ComponentDescriber::production) != true ||
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
    if (expression.arguments.size != 1) return null
    val classExpression = expression.arguments.single()
    if (
        classExpression.className != CLASS ||
            classExpression.arguments.size != 1 ||
            classExpression.refinement != null ||
            classExpression.complement
    ) {
      return null
    }
    return classExpression.arguments.single().takeIf { it.simple }
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
  internal val thisExpression = cn("This").expression
}
