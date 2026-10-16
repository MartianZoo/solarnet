package dev.martianzoo.catalog

import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.PremiseClassTable

/**
 * A signed Class selection recorded in a [GamePremise], including selected Modules and Content.
 *
 * Selections contributed by a Module may be conditional on the completed configuration. Resolved
 * premise selections are unconditional and express exact choices. Catalogs may use this generic
 * mechanism for individually overridable game items. Terraforming Mars identifies these as concrete
 * subclasses of its `Content` class; this generic selection mechanism accepts any Class.
 *
 * @property className selected class, whether included or excluded.
 * @property included true to include this class, false to exclude it.
 * @property requirement condition on the completed configuration, or null for an unconditional
 *   selection. The candidate class does not count toward its own condition.
 */
public data class ClassSelection(
    public val className: ClassName,
    public val included: Boolean = true,
    public val requirement: Requirement? = null,
) {
  /**
   * Whether this selection applies to [configuredClassNames] in a premise-local namespace over its
   * imported master. A null [requirement] always applies. Count metrics in a condition count
   * configured names that are subclasses of the named class, excluding this selection's own
   * [className]. [included] affects the selection's sign, not whether its condition is met.
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
