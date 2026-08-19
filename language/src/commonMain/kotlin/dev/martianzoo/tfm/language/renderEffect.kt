package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.InstructionTree

internal fun renderEndEffect(effect: Effect): String? {
  val condition =
      when (val trigger = effect.trigger) {
        is IfTrigger -> {
          if (!isEndTrigger(trigger.inner)) return null
          Describers.renderScoringCondition(trigger.condition) ?: return null
        }
        else -> {
          if (!isEndTrigger(trigger)) return null
          null
        }
      }
  renderPerVictoryPoints(effect.instruction)?.let {
    if (condition != null) return null
    return it
  }
  val points = Describers.renderFixedScore(effect.instruction) ?: return null
  return "$points${condition?.let { " $it" } ?: ""}."
}

internal fun isEndEffect(effect: Effect): Boolean {
  return isEndTrigger(effect.trigger)
}

private fun isEndTrigger(trigger: Trigger): Boolean =
    when (trigger) {
      is OnGainOf -> Describers.isEndTrigger(trigger.expression)
      is Trigger.Or -> trigger.triggers.all(::isEndTrigger)
      is Trigger.WrappingTrigger -> isEndTrigger(trigger.inner)
      is Trigger.OnRemoveOf,
      Trigger.WhenGain,
      Trigger.WhenRemove -> false
    }

private fun renderPerVictoryPoints(instruction: InstructionTree): String? {
  val per = instruction as? Per ?: return null
  val points = Describers.renderFixedScore(per.inner) ?: return null
  val metric = renderMetricPhrase(per.metric) ?: return null
  return "$points for $metric."
}
