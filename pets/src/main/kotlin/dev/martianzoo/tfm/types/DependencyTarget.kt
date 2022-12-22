package dev.martianzoo.tfm.types

interface DependencyTarget {
  val abstract: Boolean

  fun isSubtypeOf(that: DependencyTarget): Boolean

  /**
   * Returns the common supertype of every subtype of both `this` and `that`, if possible.
   */
  fun glb(that: DependencyTarget): DependencyTarget
}
