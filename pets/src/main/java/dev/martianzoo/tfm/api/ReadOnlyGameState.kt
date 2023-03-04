package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.util.Multiset

/** The read-only portions of [GameState]. */
interface ReadOnlyGameState {
  val setup: GameSetup
  val authority: Authority

  fun count(metric: Metric): Int

  fun count(type: Type): Int // TODO need both?

  fun getComponents(type: Type): Multiset<out Type> // TODO type, expr?

  fun evaluate(requirement: Requirement): Boolean

  fun resolve(expression: Expression): Type
}
