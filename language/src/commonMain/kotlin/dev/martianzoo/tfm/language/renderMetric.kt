package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property

internal fun renderMetricPhrase(metric: Metric, describers: Describers): String? {
  return when (metric) {
    is Metric.Count -> renderCountPhrase(metric, describers)
    is Metric.Scaled -> renderScaledCountPhrase(metric, describers)
    is Metric.Eval,
    is Metric.Max,
    is Metric.Or,
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
