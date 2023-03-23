package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.util.Multiset

/** A readable view of the state of a game in progress. */
interface GameStateReader {
  /** The initial configuration for the game. */
  val setup: GameSetup

  /** The data source used by this game. */
  val authority: Authority

  /** Returns the type represented by the (fully-transformed) [expression]. */
  fun resolve(expression: Expression): Type

  /** Determines whether the (fully-transformed) [requirement] is met in the current game state. */
  fun evaluate(requirement: Requirement): Boolean

  /** Evaluates the (fully-transformed) [metric] in the current game state. */
  fun count(metric: Metric): Int

  /** Returns the number of instances of [type] in the current game state. */
  fun count(type: Type): Int

  /** Returns the number of instances of the concrete [type] in the current game state. */
  fun countComponent(concreteType: Type): Int

  /** Returns the types of all concrete components in the current game state. */
  fun getComponents(type: Type): Multiset<out Type>
}
