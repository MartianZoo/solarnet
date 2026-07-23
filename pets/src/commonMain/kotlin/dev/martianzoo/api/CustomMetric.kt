package dev.martianzoo.api

import dev.martianzoo.pets.ast.ClassName

/** Metric behavior for a Pets [CustomClass]. */
public abstract class CustomMetric(name: String? = null) : CustomClass(name) {
  public constructor(className: ClassName) : this(className.toString())

  /**
   * Returns the virtual component count represented by [type]. The engine calls this only when the
   * custom class and all of its dependency arguments are concrete.
   */
  public abstract fun count(game: GameReader, type: Type): Int
}
