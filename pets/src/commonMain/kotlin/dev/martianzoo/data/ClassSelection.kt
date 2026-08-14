package dev.martianzoo.data

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.Requirement

/** One class inclusion or exclusion, optionally conditional on the full game configuration. */
public data class ClassSelection(
    public val className: ClassName,
    public val included: Boolean = true,
    public val requirement: Requirement? = null,
) {
  /** Whether this selection applies to the complete set of configured class names. */
  public fun appliesTo(configuredClassNames: Set<ClassName>): Boolean =
      requirement?.isMetBy { metric -> countConfigured(metric, configuredClassNames) } != false

  private fun countConfigured(metric: Metric, configuredClassNames: Set<ClassName>): Int {
    require(metric is Count && metric.expression.simple) {
      "Module conditions must count simple classes: $metric"
    }
    return if (metric.expression.className in configuredClassNames) 1 else 0
  }
}
