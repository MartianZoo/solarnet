package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpression

interface DependencyTarget {
  val abstract: Boolean

  fun isSubtypeOf(that: DependencyTarget): Boolean

  /**
   * Returns the common supertype of every subtype of both `this` and `that`, if possible.
   */
  fun glb(that: DependencyTarget): DependencyTarget
  fun toTypeExpressionFull(): TypeExpression

  val typeOnly: Boolean
  val classOnly: Boolean
}
