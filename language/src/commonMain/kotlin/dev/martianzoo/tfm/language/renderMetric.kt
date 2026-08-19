package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property

internal fun renderScoringMetric(metric: Metric): String? {
  return when (metric) {
    is Metric.Count -> {
      if (!metric.expression.simple) return null
      val (name) = tagName(metric.expression.className) ?: return null
      "each $name tag you have"
    }
    is Metric.Eval,
    is Metric.Max,
    is Metric.Or,
    is Metric.Scaled,
    is Metric.Transform,
    is Property -> null
  }
}
