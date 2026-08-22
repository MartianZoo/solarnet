package dev.martianzoo.tfm.language

import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.Dependency.Key

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
  val metric = requirement.metric as? Metric.Count ?: return null
  val expression = metric.expression
  val target = requirement.target
  renderCountedRelation(expression, this)?.let { relation ->
    if (target != 1) return null
    val objectPhrase =
        if (relation.source.ownedByYou) {
          "that you have ${indefiniteArticle(relation.source.singular)} ${relation.source.singular} " +
              "${relation.phrase} ${relation.target.linearize()}"
        } else {
          relation.asRequirement()
        }
    return requirementClause("requires", objectPhrase)
  }
  fact(expression.className, ComponentDescriber::requirement)?.minimum?.let { bound ->
    return renderRequirementBound(expression, target, bound)
  }
  return renderProductionRequirement(requirement)
      ?: renderCardResourceRequirement(requirement)
      ?: renderTagRequirement(requirement)
      ?: renderDistinctKindsRequirement(requirement)
}

private fun Describers.renderDistinctKindsRequirement(
    requirement: Requirement.Min,
): Clause? {
  val metric = requirement.metric as? Metric.Count ?: return null
  val noun = distinctOwnedKinds(metric.expression) ?: return null
  val kinds = if (requirement.target == 1) noun.singular else noun.plural
  return requirementClause("requires", "that you have ${requirement.target} $kinds")
}

private fun Describers.renderMaximum(requirement: Requirement.Max): Clause? {
  val metric = requirement.metric as? Metric.Count ?: return null
  val expression = metric.expression
  val bound = fact(expression.className, ComponentDescriber::requirement)?.maximum ?: return null
  return renderRequirementBound(expression, requirement.target, bound)
}

private fun Describers.renderRequirementGroup(requirement: Requirement.And): Clause? =
    renderTagRequirementGroup(requirement) ?: renderOwnedPlacementRequirementGroup(requirement)

private fun Describers.renderProductionRequirement(minimum: Requirement.Min): Clause? {
  if (minimum.target != 1) return null
  val metric = minimum.metric as? Metric.Count ?: return null
  val (owners, resourceClassName) = productionExpression(metric.expression) ?: return null
  if (owners.isNotEmpty()) return null
  return requirementClause(
      "requires",
      "that you have ${componentNoun(resourceClassName, 1)} production",
  )
}

private fun Describers.renderCardResourceRequirement(requirement: Requirement.Min): Clause? {
  val metric = requirement.metric as? Metric.Count ?: return null
  if (!metric.expression.simple) return null
  val noun = cardResourceNoun(metric.expression.className, requirement.target) ?: return null
  return requirementClause("requires", "that you have ${requirement.target} $noun")
}

private fun Describers.renderTagRequirement(requirement: Requirement.Min): Clause? {
  val (name) = tagName(requirement) ?: return null
  val objectPhrase =
      if (requirement.target == 1) {
        "${indefiniteArticle(name)} $name tag"
      } else {
        "${requirement.target} $name tags"
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
  val allPlanetTags = tags.all { (_, planet) -> planet }
  if (!allPlanetTags && tags.any { (_, planet) -> planet }) return null
  val nouns =
      if (allPlanetTags) tags.map { (name) -> name }
      else tags.map { (name) -> "${indefiniteArticle(name)} $name tag" }
  return requirementClause("requires", "${englishList(nouns)}${if (allPlanetTags) " tags" else ""}")
}

private fun Describers.renderOwnedPlacementRequirementGroup(requirement: Requirement.And): Clause? {
  val nouns =
      requirement.requirements.map { child ->
        val minimum = child as? Requirement.Min ?: return null
        val metric = minimum.metric as? Metric.Count ?: return null
        if (!metric.expression.simple) return null
        val noun =
            fact(metric.expression.className, ComponentDescriber::requirement)?.ownedCount
                as? ComponentDescriber.Noun.Counted ?: return null
        "${minimum.target} ${if (minimum.target == 1) noun.singular else noun.plural}"
      }
  return requirementClause("requires", "that you have ${englishList(nouns)}")
}

private fun Describers.renderRequirementBound(
    expression: Expression,
    target: Int,
    bound: ComponentDescriber.Requirement.Bound,
): Clause? {
  return when (bound) {
    is ComponentDescriber.Requirement.Bound.Threshold -> {
      if (!expression.simple) return null
      val value = renderRequirementValue(bound.value, target)
      when (bound.syntax) {
        ComponentDescriber.Requirement.ThresholdSyntax.REQUIRES_VALUE_SUBJECT ->
            requirementClause("requires", "$value ${bound.subject}")
        ComponentDescriber.Requirement.ThresholdSyntax.REQUIRES_SUBJECT_VALUE ->
            requirementClause("requires", "${bound.subject} $value")
        ComponentDescriber.Requirement.ThresholdSyntax.REQUIRES_VALUE_OR_WARMER ->
            requirementClause("requires", "$value or warmer")
        ComponentDescriber.Requirement.ThresholdSyntax.REQUIRES_HAVE_SUBJECT_OF_VALUE_OR_MORE ->
            requirementClause("requires", "that you have ${bound.subject} of $value or more")
        ComponentDescriber.Requirement.ThresholdSyntax.SUBJECT_MUST_BE_VALUE_OR_LESS ->
            requirementClause("must be", "$value or less", bound.subject)
        ComponentDescriber.Requirement.ThresholdSyntax.SUBJECT_MUST_BE_VALUE_OR_COLDER ->
            requirementClause("must be", "$value or colder", bound.subject)
      }
    }
    is ComponentDescriber.Requirement.Bound.Count -> {
      val resolved = resolveExpression(expression) ?: return null
      val ownerKey = Key(OWNED, 0)
      val unrestricted =
          resolved.hasOnlySourceDependency(ownerKey, anyoneExpression) &&
              expression.refinement == null
      val syntax =
          when {
            resolved.sourceDependencies.isEmpty() && expression.refinement == null -> bound.syntax
            unrestricted -> bound.anyoneSyntax ?: return null
            else -> return null
          }
      val noun = if (target == 1) bound.noun.singular else bound.noun.plural
      when (syntax) {
        ComponentDescriber.Requirement.CountSyntax.REQUIRES_COUNT ->
            requirementClause(
                "requires",
                if (unrestricted) "any ${if (target == 1) "" else "$target "}$noun"
                else "$target $noun",
            )
        ComponentDescriber.Requirement.CountSyntax.REQUIRES_OWNED_COUNT ->
            requirementClause("requires", "that you have $target $noun")
        ComponentDescriber.Requirement.CountSyntax.THERE_MUST_BE_COUNT_OR_FEWER ->
            requirementClause("must be", "$target or fewer ${bound.noun.plural}", "there")
        ComponentDescriber.Requirement.CountSyntax.YOU_MUST_HAVE_NO_MORE_THAN_COUNT ->
            requirementClause("must have", "no more than $target $noun", "you")
      }
    }
  }
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
