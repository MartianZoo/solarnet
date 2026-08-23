package dev.martianzoo.data

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.ClassTable

/** One class inclusion or exclusion, optionally conditional on the full game configuration. */
public data class ClassSelection(
    public val className: ClassName,
    public val included: Boolean = true,
    public val requirement: Requirement? = null,
) {
  /** Whether this selection applies to the complete set of configured class names. */
  internal fun appliesTo(
      configuredClassNames: Set<ClassName>,
      classTable: ClassTable,
  ): Boolean =
      requirement?.isMetBy { metric ->
        // A candidate selection must not satisfy or defeat its own condition merely by being the
        // selection currently under consideration.
        countConfigured(metric, configuredClassNames - className, classTable)
      } != false

  private fun countConfigured(
      metric: Metric,
      configuredClassNames: Set<ClassName>,
      classTable: ClassTable,
  ): Int {
    require(metric is Count && metric.expression.simple) {
      "Module conditions must count simple classes: $metric"
    }
    val countedClass = classTable.getClass(metric.expression.className)
    return configuredClassNames.count { configuredName ->
      classTable.getClass(configuredName).isSubtypeOf(countedClass)
    }
  }
}
