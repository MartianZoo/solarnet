package dev.martianzoo.state

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.Multiset

/**
 * Read-only queries against a live Pets world. Each call observes the current components; returned
 * multisets and sets are detached results, so later gameplay requires another query. Types are
 * interpreted in [classTable], while counts and requirements use current world state.
 */
public interface GameReader : TypeInfo {
  /** Every Actor participating in this game, with seated Players in seat order. */
  public val actors: List<Actor>

  /** Reusable source catalog from which this world's premise was resolved. */
  public val catalog: Catalog

  /** The complete class universe selected for this game. */
  override val classTable: ClassTable

  /**
   * Resolves [expression] in this world's selected class universe. The result is a type judgment,
   * independent of how many matching components currently exist; an unknown or invalid expression
   * fails rather than denoting an empty collection.
   */
  public fun resolve(expression: Expression): Type

  /** Evaluates [requirement] using current component counts and property values. */
  override fun has(requirement: Requirement): Boolean

  /**
   * Evaluates [metric] in the current world. A count whose root is a virtual `CustomMetric` calls
   * its Kotlin implementation rather than counting stored components. Undefined property reads and
   * metrics that need an absent evaluation context fail.
   */
  public fun count(metric: Metric): Int

  /**
   * Counts live components matching [type], including concrete subtypes and applicable refinements.
   * For an uninhabited type, the result is zero.
   */
  public fun count(type: Type): Int

  /**
   * Counts only instances of the exact [concreteType], excluding its subtypes. The type must be
   * concrete; an uninhabited type has count zero.
   */
  public fun countComponent(concreteType: Type): Int

  /**
   * Returns the exact types of live components matching [type], repeated according to their counts.
   * The result is a snapshot; an uninhabited or unmatched type yields an empty multiset.
   */
  public fun getComponents(type: Type): Multiset<Type>

  /**
   * Returns distinct exact types of components directly dependent on [component], excluding
   * indirect descendants. [component] must be concrete; an uninhabited component yields an empty
   * set. The result is a snapshot.
   */
  public fun getDependents(component: Type): Set<Type>

  /** Resolves [expression] with [resolve], then returns the matching component snapshot. */
  public fun getComponents(expression: Expression): Multiset<Type> =
      getComponents(resolve(expression))
}
