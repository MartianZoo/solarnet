package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal fun renderEndEffect(effect: Effect): String? {
  val trigger = effect.trigger as? OnGainOf ?: return null
  if (trigger.expression != endExpression) return null
  val gain = effect.instruction as? Gain ?: return null
  if (gain.intensity != null && gain.intensity != MANDATORY) return null
  if (!gain.gaining.simple || gain.gaining.className != victoryPoint) return null
  val count = (gain.count as? ActualScalar)?.value ?: return null
  return "$count ${if (count == 1) "VP" else "VPs"}."
}

private val endExpression = cn("End").expression
private val victoryPoint = cn("VictoryPoint")
