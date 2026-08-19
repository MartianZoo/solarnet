package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property

internal fun renderScoringMetric(metric: Metric): String? {
  return when (metric) {
    is Metric.Count -> renderScoringCount(metric)
    is Metric.Scaled -> renderScaledScoringCount(metric)
    is Metric.Eval,
    is Metric.Max,
    is Metric.Or,
    is Metric.Transform,
    is Property -> null
  }
}

private fun renderScoringCount(metric: Metric.Count): String? {
  if (metric.expression.simple) {
    val (name) = tagName(metric.expression.className) ?: return null
    return "each $name tag you have"
  }
  val resourceType = resourceOnThisCard(metric) ?: return null
  return "each ${cardResourceNoun(resourceType, 1)} on this card"
}

private fun renderScaledScoringCount(metric: Metric.Scaled): String? {
  val count = metric.inner as? Metric.Count ?: return null
  if (count.expression.simple) {
    val (name) = tagName(count.expression.className) ?: return null
    return "every ${metric.unit} $name tags you have"
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
