package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeRecord.StateChange
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScaledExpression
import kotlin.math.min

internal object LiveNodes {
  data class LiveMetric(val type: Type, val divisor: Int = 1, val max: Int = Int.MAX_VALUE) {
    fun count(game: Game) = min(game.count(type) / divisor, max)
  }

  fun from(met: Metric, game: Game): LiveMetric =
      when (met) {
        is Count -> LiveMetric(game.resolve(met.scaledEx.expression), met.scaledEx.scalar)
        is Metric.Max -> from(met.metric, game).copy(max = met.maximum)
      }

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

  fun from(trig: Trigger, game: Game): LiveTrigger {
    return when (trig) {
      is Trigger.OnGainOf -> LiveTrigger(game.resolve(trig.expression), gain = true)
      is Trigger.OnRemoveOf -> LiveTrigger(game.resolve(trig.expression), gain = false)
      is Trigger.ByTrigger -> from(trig.inner, game).copy(by = trig.by)
      else -> error("this shouldn't still be here")
    }
  }

  data class LiveTrigger(val ptype: Type, val gain: Boolean, val by: ClassName? = null) {
    fun hits(change: StateChange, game: Game): Int {
      // TODO by
      val g = if (gain) change.gaining else change.removing
      return if (g != null && game.resolve(g).isSubtypeOf(ptype)) change.count else 0
    }
  }
}
