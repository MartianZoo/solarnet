package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property

internal fun renderMetricPhrase(metric: Metric): String? {
  return when (metric) {
    is Metric.Count -> renderCountPhrase(metric)
    is Metric.Scaled -> renderScaledCountPhrase(metric)
    is Metric.Eval,
    is Metric.Max,
    is Metric.Or,
    is Metric.Transform,
    is Property -> null
  }
}

private fun renderCountPhrase(metric: Metric.Count): String? {
  if (metric.expression.simple) {
    tagName(metric.expression.className)?.let { (name) ->
      return "each $name tag you have"
    }
    cardResourceNoun(metric.expression.className, 1)?.let { noun ->
      return "each $noun you have"
    }
    placementCountPhrase(metric.expression, 1)?.let { phrase ->
      return "each $phrase"
    }
    return null
  }
  placementCountPhrase(metric.expression, 1)?.let { phrase ->
    return "each $phrase"
  }
  val resourceType = resourceOnThisCard(metric) ?: return null
  return "each ${cardResourceNoun(resourceType, 1)} on this card"
}

private fun renderScaledCountPhrase(metric: Metric.Scaled): String? {
  val count = metric.inner as? Metric.Count ?: return null
  if (count.expression.simple) {
    tagName(count.expression.className)?.let { (name) ->
      return "every ${metric.unit} $name tags you have"
    }
    cardResourceNoun(count.expression.className, metric.unit)?.let { noun ->
      return "every ${metric.unit} $noun you have"
    }
    placementCountPhrase(count.expression, metric.unit)?.let { phrase ->
      return "every ${metric.unit} $phrase"
    }
    return null
  }
  placementCountPhrase(count.expression, metric.unit)?.let { phrase ->
    return "every ${metric.unit} $phrase"
  }
  val resourceType = resourceOnThisCard(count) ?: return null
  return "every ${metric.unit} ${cardResourceNoun(resourceType, metric.unit)} on this card"
}

private fun resourceOnThisCard(metric: Metric.Count) =
    metric.expression.className.takeIf {
      metric.expression.arguments == listOf(thisExpression) &&
          metric.expression.refinement == null &&
          !metric.expression.complement &&
          cardResourceNoun(it, 1) != null
    }

private fun placementCountPhrase(
    expression: Expression,
    count: Int,
): String? {
  if (expression.refinement != null || expression.complement) return null
  val placement = Describers[expression.className].placement ?: return null
  val scope =
      when {
        expression.simple -> placement.unqualifiedMetricScope
        expression.arguments == listOf(anyoneExpression) -> placement.anyoneMetricScope
        else -> null
      } ?: return null
  val scopePhrase =
      when (scope) {
        ComponentDescriber.MetricScope.OWNED -> "you own"
        ComponentDescriber.MetricScope.IN_PLAY -> "in play"
      }
  return "${if (count == 1) placement.singular else placement.plural} $scopePhrase"
}

private val anyoneExpression = cn("Anyone").expression
private val thisExpression = cn("This").expression
