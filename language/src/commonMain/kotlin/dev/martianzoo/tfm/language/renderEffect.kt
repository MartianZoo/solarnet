package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal fun renderEndEffect(effect: Effect): String? {
  if (!isEndEffect(effect)) return null
  renderPerVictoryPoints(effect.instruction)?.let {
    return it
  }
  val (count, penalty) = fixedVictoryPoints(effect.instruction) ?: return null
  return "${if (penalty) "-" else ""}$count ${if (count == 1) "VP" else "VPs"}."
}

internal fun isEndEffect(effect: Effect): Boolean {
  val trigger = effect.trigger as? OnGainOf ?: return false
  return trigger.expression.simple && Describers[trigger.expression.className].endTrigger == true
}

private fun renderPerVictoryPoints(instruction: InstructionTree): String? {
  val per = instruction as? Per ?: return null
  val (count, penalty) = fixedVictoryPoints(per.inner) ?: return null
  if (count != 1 || penalty) return null
  val metric = renderScoringMetric(per.metric) ?: return null
  return "1 VP for $metric."
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
