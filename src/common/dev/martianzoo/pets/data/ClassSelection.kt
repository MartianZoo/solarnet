package dev.martianzoo.pets.data

import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
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
  /**
   * Whether this selection applies in a premise-local namespace over its imported master.
   *
   * @throws dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException if its authored
   *   condition is not a count of one simple class
   */
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
    if (metric !is Count || !metric.expression.simple) {
      throw InvalidPetDefinitionException("module condition must count a simple Class: `$metric`")
    }
    return configuredClassNames.count { configuredName ->
      isSubtypeOf(configuredName, metric.expression.className)
    }
  }
}
