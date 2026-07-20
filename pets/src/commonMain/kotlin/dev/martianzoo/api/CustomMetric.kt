package dev.martianzoo.api

import dev.martianzoo.pets.ast.ClassName

/** Metric behavior for a Pets [CustomClass]. */
public abstract class CustomMetric(name: String? = null) : CustomClass(name) {
  public constructor(className: ClassName) : this(className.toString())

  /**
   * Returns the virtual component count represented by [type]. Dependency arguments may be
   * abstract; the implementation decides whether and how to handle them.
   */
  public abstract fun count(game: GameReader, type: Type): Int
}
