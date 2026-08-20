package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property

internal fun renderMetricPhrase(metric: Metric, describers: Describers): String? {
  return when (metric) {
    is Metric.Count -> renderCountPhrase(metric, describers)
    is Metric.Scaled -> renderScaledCountPhrase(metric, describers)
    is Metric.Constant,
    is Metric.Eval,
    is Metric.Max,
    is Metric.Or,
    is Metric.Subtract,
    is Metric.Transform,
    is Property -> null
  }
}

private fun renderCountPhrase(metric: Metric.Count, describers: Describers): String? =
    describers.renderMetric(metric.expression)

private fun renderScaledCountPhrase(metric: Metric.Scaled, describers: Describers): String? {
  val count = metric.inner as? Metric.Count ?: return null
  return describers.renderMetric(count.expression, metric.unit)
}

internal fun Describers.renderMetric(expression: Expression, unit: Int? = null): String? {
  val count = unit ?: 1
  val prefix = unit?.let { "every $it" } ?: "each"
  if (expression.simple) {
    tagName(expression.className)?.let { (name) ->
      return "$prefix $name ${if (unit == null) "tag" else "tags"} you have"
    }
    cardResourceNoun(expression.className, count)?.let { noun ->
      return "$prefix $noun you have"
    }
    placementCountPhrase(expression, count)?.let { phrase ->
      return "$prefix $phrase"
    }
    return null
  }
  placementCountPhrase(expression, count)?.let { phrase ->
    return "$prefix $phrase"
  }
  if (
      expression.arguments != listOf(thisExpression) ||
          expression.refinement != null ||
          expression.complement
  ) {
    return null
  }
  val noun = cardResourceNoun(expression.className, count) ?: return null
  return "$prefix $noun on this card"
}

private fun Describers.placementCountPhrase(expression: Expression, count: Int): String? {
  if (expression.refinement != null || expression.complement) return null
  val placement = fact(expression.className, ComponentDescriber::placement) ?: return null
  val (owner, location) =
      when {
        expression.simple -> (placement.unqualifiedMetricOwner ?: return null) to null
        expression.arguments == listOf(anyoneExpression) ->
            (placement.anyoneMetricOwner ?: return null) to null
        expression.arguments.size == 2 && expression.arguments.last() == anyoneExpression -> {
          val location = expression.arguments.first()
          if (!location.simple) return null
          (placement.anyoneMetricOwner ?: return null) to
              (fact(location.className, ComponentDescriber::metricLocation) ?: return null)
        }
        else -> null
      } ?: return null
  val ownerPhrase =
      when (owner) {
        ComponentDescriber.MetricOwner.YOU -> " you own"
        ComponentDescriber.MetricOwner.ANY_PLAYER -> ""
      }
  val noun = if (count == 1) placement.singular else placement.plural
  return "$noun$ownerPhrase${location?.let { " $it" }.orEmpty()}"
}
