package dev.martianzoo.pets.data

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.PremiseClassTable

/** One class inclusion or exclusion, optionally conditional on the full game configuration. */
public data class ClassSelection(
    public val className: ClassName,
    public val included: Boolean = true,
    public val requirement: Requirement? = null,
) {
  /** Whether this selection applies in a premise-local namespace over its imported master. */
  public fun appliesTo(
      configuredClassNames: Set<ClassName>,
      classTable: PremiseClassTable,
  ): Boolean = appliesTo(configuredClassNames, classTable::isSubtypeOf)

  private fun appliesTo(
      configuredClassNames: Set<ClassName>,
      isSubtypeOf: (candidate: ClassName, superclass: ClassName) -> Boolean,
  ): Boolean =
      requirement?.isMetBy { metric ->
        // A candidate selection must not satisfy or defeat its own condition merely by being the
        // selection currently under consideration.
        countConfigured(metric, configuredClassNames - className, isSubtypeOf)
      } != false

  private fun countConfigured(
      metric: Metric,
      configuredClassNames: Set<ClassName>,
      isSubtypeOf: (candidate: ClassName, superclass: ClassName) -> Boolean,
  ): Int {
    require(metric is Count && metric.expression.simple) {
      "Module conditions must count simple classes: $metric"
    }
    return configuredClassNames.count { configuredName ->
      isSubtypeOf(configuredName, metric.expression.className)
    }
  }
}
