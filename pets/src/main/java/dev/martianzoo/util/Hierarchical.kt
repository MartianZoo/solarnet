package dev.martianzoo.util

interface Hierarchical<H : Hierarchical<H>> {
  val abstract: Boolean

  fun isSubtypeOf(that: H): Boolean

  @Suppress("UNCHECKED_CAST")
  fun isSupertypeOf(that: H): Boolean = that.isSubtypeOf(this as H)

  infix fun glb(that: H): H?

  infix fun lub(that: H): H
}
