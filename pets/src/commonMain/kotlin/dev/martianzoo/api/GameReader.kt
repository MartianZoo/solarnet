package dev.martianzoo.api

import dev.martianzoo.data.Ruleset
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.types.Type
import dev.martianzoo.util.Multiset

/** A readable view of a world. */
public interface GameReader : TypeInfo {
  /** The initial configuration for the game. */
  public val setup: GameSetup

  /** The resolved ruleset used by the game. */
  public val ruleset: Ruleset

  /** Returns the type represented by the (fully-prepared) [expression]. */
  public fun resolve(expression: Expression): Type

  /** Determines whether the (fully-prepared) [requirement] is met in the current world. */
  override fun has(requirement: Requirement): Boolean

  /**
   * Evaluates the (fully-prepared) [metric] in the current world. A count whose root is a custom
   * class is computed by that Kotlin implementation rather than from components.
   */
  public fun count(metric: Metric): Int

  /** Returns the number of component instances having type [type] in the current world. */
  public fun count(type: Type): Int

  public fun containsAny(type: Type): Boolean

  /** Returns the number of instances of [concreteType] in the current world. */
  public fun countComponent(concreteType: Type): Int

  /** Returns the types of all concrete components in the current world. */
  public fun getComponents(type: Type): Multiset<Type>

  /** Returns the types of all concrete components matching the Pets type expression [type]. */
  public fun getComponents(type: String): Multiset<Type> = getComponents(resolve(parse(type)))
}
