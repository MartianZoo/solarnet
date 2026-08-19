package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal fun renderEndEffect(effect: Effect): String? {
  val trigger = effect.trigger as? OnGainOf ?: return null
  if (trigger.expression != endExpression) return null
  val (count, penalty) = fixedVictoryPoints(effect.instruction) ?: return null
  return "${if (penalty) "-" else ""}$count ${if (count == 1) "VP" else "VPs"}."
}

private fun fixedVictoryPoints(instruction: InstructionTree): Pair<Int, Boolean>? {
  return when (instruction) {
    is Gain -> {
      if (instruction.intensity != null && instruction.intensity != MANDATORY) return null
      if (!instruction.gaining.simple || instruction.gaining.className != victoryPoint) return null
      val count = (instruction.count as? ActualScalar)?.value ?: return null
      count to false
    }
    is Remove -> {
      if (instruction.intensity != null && instruction.intensity != MANDATORY) return null
      if (!instruction.removing.simple || instruction.removing.className != victoryPoint)
          return null
      val count = (instruction.count as? ActualScalar)?.value ?: return null
      count to true
    }
    else -> null
  }
}

private val endExpression = cn("End").expression
private val victoryPoint = cn("VictoryPoint")
