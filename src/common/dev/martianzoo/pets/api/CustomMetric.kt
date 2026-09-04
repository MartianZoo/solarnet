package dev.martianzoo.pets.api

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.types.Type

/** Metric behavior for a Pets [CustomClass]. */
public abstract class CustomMetric(name: String? = null) : CustomClass(name) {
  private constructor(className: ClassName) : this(className.toString())

  /**
   * Returns the virtual component count represented by the concrete [type]. For an abstract custom
   * metric type, the engine calls this for every concrete specialization whose dependency targets
   * exist in the game and sums the results.
   */
  public abstract fun count(game: GameReader, type: Type): Int
}
