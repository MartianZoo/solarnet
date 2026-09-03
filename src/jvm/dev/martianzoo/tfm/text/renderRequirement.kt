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
      renderLoweredRequirement(lowerProductionSyntax(requirement), describers)
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
          "${indefiniteArticle(relation.source.singular)} ${relation.source.singular} " +
              "${relation.phrase} ${relation.target.linearize()}"
        } else {
          relation.asRequirement()
        }
    return requirementClause("requires", objectPhrase)
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
  val kinds = if (requirement.target == 1) noun.singular else noun.plural
  val amount =
      if (requirement.target == 1) "${indefiniteArticle(kinds)} $kinds"
      else "${requirement.target} $kinds"
  return requirementClause("requires", amount)
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
      "requires",
      "that you have ${componentNoun(production.resource, 1)} production",
  )
}

private fun Describers.renderCardResourceRequirement(requirement: Requirement.Min): Clause? {
  val expression = countedExpression(requirement) ?: return null
  if (!expression.simple) return null
  val noun = cardResourceNoun(expression.className, requirement.target) ?: return null
  val amount =
      if (requirement.target == 1) {
        "${indefiniteArticle(noun)} $noun"
      } else "${requirement.target} $noun"
  return requirementClause("requires", amount)
}

private fun Describers.renderTagRequirement(requirement: Requirement.Min): Clause? {
  val (name) = tagName(requirement) ?: return null
  val objectPhrase =
      if (requirement.target == 1) {
        "${indefiniteArticle(name)} $name tag"
      } else {
        "${requirement.target} $name ${if (requirement.target == 1) "tag" else "tags"}"
      }
  return requirementClause("requires", objectPhrase)
}

private fun Describers.renderTagRequirementGroup(requirement: Requirement.And): Clause? {
  val tags =
      requirement.requirements.map { child ->
        val minimum = child as? Requirement.Min ?: return null
        if (minimum.target != 1) return null
        tagName(minimum) ?: return null
      }
  val allPlanetaryTags = tags.all { (_, planetary) -> planetary }
  if (!allPlanetaryTags && tags.any { (_, planetary) -> planetary }) return null
  val nouns = tags.map { (name) -> "${indefiniteArticle(name)} $name tag" }
  return requirementClause("requires", englishList(nouns))
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
        if (minimum.target == 1) {
          "${indefiniteArticle(noun.singular)} ${noun.singular}"
        } else {
          "${minimum.target} ${noun.plural}"
        }
      }
  return requirementClause("requires", englishList(nouns))
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
  return requirementClause("requires", "that ${bound.subject} is $value or $comparison")
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
          owned -> {
            val amount =
                if (target == 1) {
                  "${indefiniteArticle(noun)} $noun"
                } else {
                  "$target $noun"
                }
            requirementClause("requires", "that you have $amount")
          }
          explicitlyAnyOwner -> {
            val amount = if (target == 1) "any $noun" else "any $target $noun"
            requirementClause("requires", amount)
          }
          else -> requirementClause("requires", "$target $noun")
        }
    BoundDirection.MAXIMUM ->
        when {
          owned ->
              requirementClause("requires", "that you have $target or fewer ${bound.noun.plural}")
          else ->
              requirementClause("requires", "that there are $target or fewer ${bound.noun.plural}")
        }
  }
}

private enum class BoundDirection {
  MINIMUM,
  MAXIMUM,
}

private fun requirementClause(
    verb: String,
    objectPhrase: String,
    subject: String? = null,
): Clause.Simple =
    Clause.Simple(
        predicate = Predicate(verb, Coordination.one(NounPhrase.text(objectPhrase))),
        subject = subject?.let(NounPhrase::text),
    )

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
