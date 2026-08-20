package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement

internal fun renderRequirement(requirement: Requirement, describers: Describers): String? =
    renderLoweredRequirement(lowerProductionSyntax(requirement), describers)

private fun renderLoweredRequirement(requirement: Requirement, describers: Describers): String? =
    when (requirement) {
      is Requirement.Min -> renderMinimum(requirement, describers)
      is Requirement.Max -> renderMaximum(requirement, describers)
      is Requirement.And -> describers.renderRequirementGroup(requirement)
      is Requirement.Eval,
      is Requirement.Exact,
      is Requirement.Or -> null
      is Requirement.Transform -> null
    }

private fun renderMinimum(requirement: Requirement.Min, describers: Describers): String? =
    describers.renderMinimum(requirement)

private fun renderMaximum(requirement: Requirement.Max, describers: Describers): String? =
    describers.renderMaximum(requirement)

private fun Describers.renderMinimum(requirement: Requirement.Min): String? {
  val metric = requirement.metric as? Metric.Count ?: return null
  val expression = metric.expression
  val target = requirement.target
  this[expression.className].requirement?.minimum?.let { bound ->
    return renderRequirementBound(expression, target, bound)
  }
  return renderProductionRequirement(requirement)
      ?: renderCardResourceRequirement(requirement)
      ?: renderTagRequirement(requirement)
}

private fun Describers.renderMaximum(requirement: Requirement.Max): String? {
  val metric = requirement.metric as? Metric.Count ?: return null
  val expression = metric.expression
  val bound = this[expression.className].requirement?.maximum ?: return null
  return renderRequirementBound(expression, requirement.target, bound)
}

private fun Describers.renderRequirementGroup(requirement: Requirement.And): String? =
    renderTagRequirementGroup(requirement) ?: renderOwnedPlacementRequirementGroup(requirement)

private fun Describers.renderProductionRequirement(minimum: Requirement.Min): String? {
  if (minimum.target != 1) return null
  val metric = minimum.metric as? Metric.Count ?: return null
  val (owners, resourceClassName) = productionExpression(metric.expression) ?: return null
  if (owners.isNotEmpty()) return null
  return "Requires that you have ${componentNoun(resourceClassName, 1)} production."
}

private fun Describers.renderCardResourceRequirement(requirement: Requirement.Min): String? {
  val metric = requirement.metric as? Metric.Count ?: return null
  if (!metric.expression.simple) return null
  val noun = cardResourceNoun(metric.expression.className, requirement.target) ?: return null
  return "Requires that you have ${requirement.target} $noun."
}

private fun Describers.renderTagRequirement(requirement: Requirement.Min): String? {
  val (name) = tagName(requirement) ?: return null
  return if (requirement.target == 1) {
    "Requires ${indefiniteArticle(name)} $name tag."
  } else {
    "Requires ${requirement.target} $name tags."
  }
}

private fun Describers.renderTagRequirementGroup(requirement: Requirement.And): String? {
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
  return "Requires ${englishList(nouns)}${if (allPlanetTags) " tags" else ""}."
}

private fun Describers.renderOwnedPlacementRequirementGroup(requirement: Requirement.And): String? {
  val nouns =
      requirement.requirements.map { child ->
        val minimum = child as? Requirement.Min ?: return null
        val metric = minimum.metric as? Metric.Count ?: return null
        if (!metric.expression.simple) return null
        val noun =
            this[metric.expression.className].requirement?.ownedCount
                as? ComponentDescriber.Noun.Counted ?: return null
        "${minimum.target} ${if (minimum.target == 1) noun.singular else noun.plural}"
      }
  return "Requires that you have ${englishList(nouns)}."
}

private fun Describers.renderRequirementBound(
    expression: Expression,
    target: Int,
    bound: ComponentDescriber.Requirement.Bound,
): String? {
  return when (bound) {
    is ComponentDescriber.Requirement.Bound.Threshold -> {
      if (!expression.simple) return null
      val value = renderRequirementValue(bound.value, target)
      when (bound.syntax) {
        ComponentDescriber.Requirement.ThresholdSyntax.REQUIRES_VALUE_SUBJECT ->
            "Requires $value ${bound.subject}."
        ComponentDescriber.Requirement.ThresholdSyntax.REQUIRES_SUBJECT_VALUE ->
            "Requires ${bound.subject} $value."
        ComponentDescriber.Requirement.ThresholdSyntax.REQUIRES_VALUE_OR_WARMER ->
            "Requires $value or warmer."
        ComponentDescriber.Requirement.ThresholdSyntax.REQUIRES_HAVE_SUBJECT_OF_VALUE_OR_MORE ->
            "Requires that you have ${bound.subject} of $value or more."
        ComponentDescriber.Requirement.ThresholdSyntax.SUBJECT_MUST_BE_VALUE_OR_LESS ->
            "${bound.subject} must be $value or less."
        ComponentDescriber.Requirement.ThresholdSyntax.SUBJECT_MUST_BE_VALUE_OR_COLDER ->
            "${bound.subject} must be $value or colder."
      }
    }
    is ComponentDescriber.Requirement.Bound.Count -> {
      val syntax =
          when {
            expression.simple -> bound.syntax
            expression.arguments == listOf(anyoneExpression) &&
                expression.refinement == null &&
                !expression.complement -> bound.anyoneSyntax ?: return null
            else -> return null
          }
      val noun = if (target == 1) bound.noun.singular else bound.noun.plural
      when (syntax) {
        ComponentDescriber.Requirement.CountSyntax.REQUIRES_COUNT -> "Requires $target $noun."
        ComponentDescriber.Requirement.CountSyntax.REQUIRES_OWNED_COUNT ->
            "Requires that you have $target $noun."
        ComponentDescriber.Requirement.CountSyntax.THERE_MUST_BE_COUNT_OR_FEWER ->
            "There must be $target or fewer ${bound.noun.plural}."
        ComponentDescriber.Requirement.CountSyntax.YOU_MUST_HAVE_NO_MORE_THAN_COUNT ->
            "You must have no more than $target $noun."
      }
    }
  }
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
