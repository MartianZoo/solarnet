package dev.martianzoo.pets.api

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.types.Type

/** Metric behavior for a Pets [CustomClass]. */
public abstract class CustomMetric(name: String? = null) : CustomClass(name) {
  private constructor(className: ClassName) : this(className.toString())

  /**
   * Computes one abstract custom-metric query directly, or returns null to request ordinary
   * concrete specialization. Implementations should override this only when they can evaluate the
   * complete query more directly than enumerating its dependency cross-product.
   */
  public open fun countAbstract(game: GameReader, type: Type): Int? = null

  /**
   * Returns the virtual component count represented by the concrete [type]. For an abstract custom
   * metric type, the engine calls this for every concrete specialization whose dependency targets
   * exist in the game and sums the results.
   */
  public abstract fun count(game: GameReader, type: Type): Int
}
