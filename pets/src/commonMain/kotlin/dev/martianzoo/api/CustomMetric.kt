package dev.martianzoo.api

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.types.Type

/** Metric behavior for a Pets [CustomClass]. */
public abstract class CustomMetric(name: String? = null) : CustomClass(name) {
  public constructor(className: ClassName) : this(className.toString())

  /**
   * Returns the virtual component count represented by the concrete [type]. For an abstract custom
   * metric type, the engine calls this once for every concrete subtype and sums the results.
   */
  public abstract fun count(game: GameReader, type: Type): Int
}
