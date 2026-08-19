package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal fun renderEndEffect(effect: Effect): String? {
  val condition =
      when (val trigger = effect.trigger) {
        is IfTrigger -> {
          if (!isEndTrigger(trigger.inner)) return null
          renderScoringCondition(trigger.condition) ?: return null
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
  val (count, penalty) = fixedVictoryPoints(effect.instruction) ?: return null
  val points = "${if (penalty) "-" else ""}$count ${if (count == 1) "VP" else "VPs"}"
  return "$points${condition?.let { " $it" } ?: ""}."
}

internal fun isEndEffect(effect: Effect): Boolean {
  return isEndTrigger(effect.trigger)
}

private fun isEndTrigger(trigger: Trigger): Boolean =
    when (trigger) {
      is OnGainOf ->
          trigger.expression.simple && Describers[trigger.expression.className].endTrigger == true
      is Trigger.Or -> trigger.triggers.all(::isEndTrigger)
      is Trigger.WrappingTrigger -> isEndTrigger(trigger.inner)
      is Trigger.OnRemoveOf,
      Trigger.WhenGain,
      Trigger.WhenRemove -> false
    }

private fun renderPerVictoryPoints(instruction: InstructionTree): String? {
  val per = instruction as? Per ?: return null
  val (count, penalty) = fixedVictoryPoints(per.inner) ?: return null
  val metric = renderScoringMetric(per.metric) ?: return null
  val points = "${if (penalty) "-" else ""}$count ${if (count == 1) "VP" else "VPs"}"
  return "$points for $metric."
}

private fun fixedVictoryPoints(instruction: InstructionTree): Pair<Int, Boolean>? {
  return when (instruction) {
    is Gain -> {
      if (instruction.intensity != null && instruction.intensity != MANDATORY) return null
      if (
          !instruction.gaining.simple ||
              Describers[instruction.gaining.className].victoryPoint != true
      ) {
        return null
      }
      val count = (instruction.count as? ActualScalar)?.value ?: return null
      count to false
    }
    is Remove -> {
      if (instruction.intensity != null && instruction.intensity != MANDATORY) return null
      if (
          !instruction.removing.simple ||
              Describers[instruction.removing.className].victoryPoint != true
      ) {
        return null
      }
      val count = (instruction.count as? ActualScalar)?.value ?: return null
      count to true
    }
    else -> null
  }
}
