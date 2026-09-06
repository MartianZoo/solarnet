package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key

/** The component expression counted by a cardinality requirement, if it directly counts one. */
internal fun countedExpression(requirement: Requirement.Counting): Expression? =
    (requirement.metric as? Metric.Count)?.expression

internal fun renderRequirement(
    requirement: Requirement,
    describers: Describers,
): Rendering<String> {
  val rendered =
      renderLoweredRequirement(describers.lowerProductionSyntax(requirement), describers)
          ?.let(::Sentence)
          ?.linearize()
  return rendered?.let { Rendering.resolved(it) }
      ?: Rendering.unresolved(
          requirement,
          RefusalReason.UNKNOWN_REQUIREMENT_FRAME,
          completeSentence("[$requirement]"),
      )
}

private fun renderLoweredRequirement(requirement: Requirement, describers: Describers): Clause? =
    when (requirement) {
      is Requirement.Min -> renderMinimum(requirement, describers)
      is Requirement.Max -> renderMaximum(requirement, describers)
      is Requirement.And -> describers.renderRequirementGroup(requirement)
      is Requirement.Eval,
      is Requirement.Exact,
      is Requirement.Or -> null
      is Requirement.Transform -> null
    }

private fun renderMinimum(requirement: Requirement.Min, describers: Describers): Clause? =
    describers.renderMinimum(requirement)

private fun renderMaximum(requirement: Requirement.Max, describers: Describers): Clause? =
    describers.renderMaximum(requirement)

private fun Describers.renderMinimum(requirement: Requirement.Min): Clause? {
  val expression = countedExpression(requirement) ?: return null
  val target = requirement.target
  renderCountedRelation(expression, this)?.let { relation ->
    if (target != 1) return null
    val objectPhrase =
        if (relation.source.ownedByYou) {
          relation.source
              .referenceWithoutOwnership(Determiner.INDEFINITE)
              .withModifier(Modifier.Relation(relation.phrase, relation.target.reference()))
        } else {
          relation.asRequirement()
        }
    return requirementClause(objectPhrase)
  }
  fact(expression.className, ComponentDescriber::requirement)?.minimum?.let { bound ->
    return renderRequirementBound(expression, target, bound, BoundDirection.MINIMUM)
  }
  return renderProductionRequirement(requirement)
      ?: renderCardResourceRequirement(requirement)
      ?: renderTagRequirement(requirement)
      ?: renderDistinctKindsRequirement(requirement)
}

private fun Describers.renderDistinctKindsRequirement(
    requirement: Requirement.Min,
): Clause? {
  val expression = countedExpression(requirement) ?: return null
  val noun = distinctOwnedKinds(expression, this) ?: return null
  return requirementClause(quantifiedNoun(noun, requirement.target))
}

private fun Describers.renderMaximum(requirement: Requirement.Max): Clause? {
  val expression = countedExpression(requirement) ?: return null
  val bound = fact(expression.className, ComponentDescriber::requirement)?.maximum ?: return null
  return renderRequirementBound(
      expression,
      requirement.target,
      bound,
      BoundDirection.MAXIMUM,
  )
}

private fun Describers.renderRequirementGroup(requirement: Requirement.And): Clause? =
    renderTagRequirementGroup(requirement) ?: renderOwnedPlacementRequirementGroup(requirement)

private fun Describers.renderProductionRequirement(minimum: Requirement.Min): Clause? {
  if (minimum.target != 1) return null
  val expression = countedExpression(minimum) ?: return null
  val production = productionExpression(expression, this) ?: return null
  if (production.owner != null) return null
  return requirementClause(
      NounPhrase.you(),
      Verb.HAVE,
      NounPhrase.text("${componentNoun(production.resource, 1)} production"),
  )
}

private fun Describers.renderCardResourceRequirement(requirement: Requirement.Min): Clause? {
  val expression = countedExpression(requirement) ?: return null
  if (!expression.simple) return null
  val noun = cardResourceNounPhrase(expression.className, requirement.target) ?: return null
  val quantified =
      if (requirement.target == 1) {
        noun.copy(count = null, determiner = Determiner.INDEFINITE)
      } else {
        noun
      }
  return requirementClause(quantified)
}

private fun Describers.renderTagRequirement(requirement: Requirement.Min): Clause? {
  val name = tagName(requirement) ?: return null
  return requirementClause(
      quantifiedNoun(
          ComponentDescriber.Noun.Counted("$name tag", "$name tags"),
          requirement.target,
      )
  )
}

private fun Describers.renderTagRequirementGroup(requirement: Requirement.And): Clause? {
  val tags =
      requirement.requirements.map { child ->
        val minimum = child as? Requirement.Min ?: return null
        if (minimum.target != 1) return null
        tagName(minimum) ?: return null
      }
  val nouns = tags.map { name ->
    NounPhrase("$name tag", determiner = Determiner.INDEFINITE)
  }
  return requirementClause(Coordination(nouns, Conjunction.AND))
}

private fun Describers.renderOwnedPlacementRequirementGroup(requirement: Requirement.And): Clause? {
  val nouns =
      requirement.requirements.map { child ->
        val minimum = child as? Requirement.Min ?: return null
        val expression = countedExpression(minimum) ?: return null
        if (!expression.simple) return null
        val noun =
            fact(expression.className, ComponentDescriber::requirement)?.ownedCount
                as? ComponentDescriber.Noun.Counted ?: return null
        quantifiedNoun(noun, minimum.target)
      }
  return requirementClause(Coordination(nouns, Conjunction.AND))
}

private fun Describers.renderRequirementBound(
    expression: Expression,
    target: Int,
    bound: ComponentDescriber.Requirement.Bound,
    direction: BoundDirection,
): Clause? {
  return when (bound) {
    is ComponentDescriber.Requirement.Bound.Threshold ->
        renderThresholdBound(expression, target, bound, direction)
    is ComponentDescriber.Requirement.Bound.Count ->
        renderCountBound(expression, target, bound, direction)
  }
}

private fun Describers.renderThresholdBound(
    expression: Expression,
    target: Int,
    bound: ComponentDescriber.Requirement.Bound.Threshold,
    direction: BoundDirection,
): Clause? {
  if (!expression.simple) return null
  val value = renderRequirementValue(bound.value, target)
  val comparison = if (direction == BoundDirection.MINIMUM) "higher" else "lower"
  return requirementClause(
      NounPhrase.text(bound.subject),
      Verb.BE,
      Coordination(
          listOf(NounPhrase.text(value), NounPhrase.text(comparison)),
          Conjunction.OR,
      ),
  )
}

private fun Describers.renderCountBound(
    expression: Expression,
    target: Int,
    bound: ComponentDescriber.Requirement.Bound.Count,
    direction: BoundDirection,
): Clause? {
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  if (expression.refinement != null) return null
  val explicitlyAnyOwner = resolved.hasOnlySourceDependency(ownerKey, anyoneExpression)
  val owned =
      when {
        resolved.sourceDependencies.isEmpty() -> isPlayerOwned(expression.className)
        explicitlyAnyOwner -> false
        else -> return null
      }
  val noun = if (target == 1) bound.noun.singular else bound.noun.plural
  return when (direction) {
    BoundDirection.MINIMUM ->
        when {
          owned ->
              requirementClause(
                  NounPhrase.you(),
                  Verb.HAVE,
                  quantifiedNoun(bound.noun, target),
              )
          explicitlyAnyOwner -> {
            val amount =
                if (target == 1) {
                  NounPhrase(noun, determiner = Determiner.ANY)
                } else {
                  NounPhrase.text("any $target $noun")
                }
            requirementClause(amount)
          }
          else -> requirementClause(NounPhrase(noun, count = target))
        }
    BoundDirection.MAXIMUM ->
        when {
          owned ->
              requirementClause(
                  NounPhrase.you(),
                  Verb.HAVE,
                  NounPhrase.text("$target or fewer ${bound.noun.plural}"),
              )
          else ->
              requirementClause(
                  NounPhrase.plural("there"),
                  Verb.BE,
                  NounPhrase.text("$target or fewer ${bound.noun.plural}"),
              )
        }
  }
}

private enum class BoundDirection {
  MINIMUM,
  MAXIMUM,
}

private fun requirementClause(objectPhrase: NounPhrase): Clause.Simple =
    requirementClause(Coordination.one(objectPhrase))

private fun requirementClause(objects: Coordination<NounPhrase>): Clause.Simple =
    Clause.Simple(
        predicate = Predicate(Verb("requires"), objects),
    )

private fun requirementClause(
    subject: NounPhrase,
    verb: Verb,
    objectPhrase: NounPhrase,
): Clause.Simple = requirementClause(subject, verb, Coordination.one(objectPhrase))

private fun requirementClause(
    subject: NounPhrase,
    verb: Verb,
    objects: Coordination<NounPhrase>,
): Clause.Simple =
    requirementClause(
        Clause.Simple(
            Predicate(verb, objects),
            subject,
        )
    )

private fun requirementClause(complement: Clause): Clause.Simple =
    Clause.Simple(
        predicate =
            Predicate(
                Verb("requires"),
                complement = Predicate.Complement(complement),
            ),
    )

private fun Describers.quantifiedNoun(
    noun: ComponentDescriber.Noun.Counted,
    count: Int,
): NounPhrase =
    if (count == 1) {
      NounPhrase(noun.singular, noun.plural, determiner = Determiner.INDEFINITE)
    } else {
      NounPhrase(noun.singular, noun.plural, count = count)
    }

private fun renderRequirementValue(
    value: ComponentDescriber.Requirement.Value,
    target: Int,
): String =
    when (value) {
      ComponentDescriber.Requirement.Value.PLAIN -> target.toString()
      ComponentDescriber.Requirement.Value.PERCENT -> "$target%"
      ComponentDescriber.Requirement.Value.DOUBLE_PERCENT -> "${target * 2}%"
      ComponentDescriber.Requirement.Value.TEMPERATURE -> {
        val degrees = -30 + 2 * target
        "${if (degrees > 0) "+" else ""}${degrees}°C"
      }
    }
