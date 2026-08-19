package dev.martianzoo.tfm.language

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

private fun renderCountPhrase(metric: Metric.Count): String? =
    Describers.renderMetric(metric.expression)

private fun renderScaledCountPhrase(metric: Metric.Scaled): String? {
  val count = metric.inner as? Metric.Count ?: return null
  return Describers.renderMetric(count.expression, metric.unit)
}
