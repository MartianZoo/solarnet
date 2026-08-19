package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
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
    return null
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
    return null
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

private val thisExpression = cn("This").expression
