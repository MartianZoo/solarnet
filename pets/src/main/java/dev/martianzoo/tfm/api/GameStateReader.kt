package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.util.Multiset

interface GameStateReader {
  val setup: GameSetup
  val authority: Authority

  fun resolve(expression: Expression): Type

  fun evaluate(requirement: Requirement): Boolean

  fun count(metric: Metric): Int

  fun count(type: Type): Int

  fun getComponents(type: Type): Multiset<out Type>
}
