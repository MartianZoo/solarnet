package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScaledExpression

internal object LiveNodes {
  fun from(reqt: Requirement, game: Game): LiveRequirement {
    fun count(scaledEx: ScaledExpression) = game.count(game.resolve(scaledEx.expression))

    return when (reqt) {
      is Min -> LiveRequirement { count(reqt.scaledEx) >= reqt.scaledEx.scalar }
      is Requirement.Max -> LiveRequirement { count(reqt.scaledEx) <= reqt.scaledEx.scalar }
      is Exact -> LiveRequirement { count(reqt.scaledEx) == reqt.scaledEx.scalar }
      is Requirement.Or -> {
        val reqts = reqt.requirements.toList().map { from(it, game) }
        LiveRequirement { reqts.any { it.evaluate(game) } }
      }
      is Requirement.And -> {
        val reqts = reqt.requirements.map { from(it, game) }
        LiveRequirement { reqts.all { it.evaluate(game) } }
      }
      is Requirement.Transform -> error("should have been transformed by now")
    }
  }

  class LiveRequirement(private val evaluator: (Game) -> Boolean) {
    fun evaluate(game: Game) = evaluator(game)
  }
}
