package dev.martianzoo.engine

import dev.martianzoo.api.TypeInfo
import dev.martianzoo.types.Type
import dev.martianzoo.util.Multiset

/**
 * A multiset of [Component] instances; the "present" state of a game in progress. It is a plain
 * multiset, but called a "graph" because these component instances have references to their
 * dependencies which are also stored in the multiset.
 */
public interface ComponentGraph {

  /**
   * Does at least one instance of [component] exist currently? (That is, is [countComponent]
   * nonzero?
   */
  public operator fun contains(component: Component): Boolean

  /** How many instances of the exact component [component] currently exist? */
  public fun countComponent(component: Component): Int

  /**
   * How many total component instances have the type [parentType] (or any of its subtypes)? Returns
   * zero for a phantom type, which cannot have stored components.
   */
  public fun count(parentType: Type, info: TypeInfo): Int

  public fun containsAny(parentType: Type, info: TypeInfo): Boolean

  /**
   * Returns all component instances having the type [parentType] (or any of its subtypes), as a
   * multiset. The size of the returned collection will be `[count]([parentType])` . A phantom type
   * returns an empty multiset. If [parentType] is `Component` this returns the entire component
   * multiset.
   */
  public fun getAll(parentType: Type, info: TypeInfo): Multiset<Component>
}
