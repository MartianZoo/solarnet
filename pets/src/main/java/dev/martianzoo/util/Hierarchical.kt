package dev.martianzoo.util

/**
 * An object with hierarchical structure (subtypes and supertypes). It is assumed that there is a
 * single root to this inheritance tree.
 */
interface Hierarchical<H : Hierarchical<H>> : Reifiable<H> {
  /** Returns `true` if `this` is a specific form of `that` (or they are the same). */
  fun isSubtypeOf(that: H): Boolean

  /** Returns `true` if `this` is a general form of `that` (or they are the same). */
  fun isSupertypeOf(that: H): Boolean {
    @Suppress("UNCHECKED_CAST") return that.isSubtypeOf(this as H)
  }

  /** Returns the nearest common subtype of `this` and [that], if possible */
  infix fun glb(that: H): H?

  /** Returns the nearest common supertype of `this` and [that]. */
  infix fun lub(that: H): H

  companion object {
    /**
     * Returns the nearest common subtype of all the elements in [list], if possible. Returns `null`
     * if either the list is empty or there is no common subtype.
     */
    fun <H : Hierarchical<H>> glb(list: Collection<H>): H? =
        list.reduceOrNull { a, b -> (a glb b)!! }

    /**
     * Returns the nearest common supertype of all the elements in [list], or `null` if the list is
     * empty.
     */
    fun <H : Hierarchical<H>> lub(list: Collection<H>): H? = list.reduceOrNull { a, b -> a lub b }
  }
}
