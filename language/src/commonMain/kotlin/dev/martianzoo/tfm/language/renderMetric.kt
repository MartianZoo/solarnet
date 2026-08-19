package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property

internal fun renderScoringMetric(metric: Metric): String? {
  return when (metric) {
    is Metric.Count -> {
      if (metric.expression.simple) {
        val (name) = tagName(metric.expression.className) ?: return null
        "each $name tag you have"
      } else if (
          metric.expression.className == animal &&
              metric.expression.arguments == listOf(thisExpression) &&
              metric.expression.refinement == null &&
              !metric.expression.complement
      ) {
        "each animal on this card"
      } else {
        null
      }
    }
    is Metric.Eval,
    is Metric.Max,
    is Metric.Or,
    is Metric.Scaled,
    is Metric.Transform,
    is Property -> null
  }
}

private val animal = cn("Animal")
private val thisExpression = cn("This").expression
