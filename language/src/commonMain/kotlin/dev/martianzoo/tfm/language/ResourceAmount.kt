package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

/** A concrete standard-resource amount retained across payment constructions. */
internal data class ResourceAmount(val count: Int, val noun: String)

internal fun owedReduction(
    instruction: InstructionTree,
    describers: Describers,
): ResourceAmount? {
  val removal = instruction as? Remove ?: return null
  return paymentResourceAmount(
      removal.removing,
      removal.count,
      removal.intensity,
      ComponentDescriber.PaymentRole.OWED,
      describers,
  )
}

internal fun paymentResourceGain(
    instruction: InstructionTree,
    role: ComponentDescriber.PaymentRole,
    describers: Describers,
): ResourceAmount? {
  val gain = instruction as? Gain ?: return null
  return paymentResourceAmount(gain.gaining, gain.count, gain.intensity, role, describers)
}

private fun paymentResourceAmount(
    expression: Expression,
    scalar: Scalar,
    intensity: Intensity?,
    role: ComponentDescriber.PaymentRole,
    describers: Describers,
): ResourceAmount? {
  if (intensity != null && intensity != MANDATORY) return null
  if (expression.refinement != null || expression.complement) return null
  if (describers.fact(expression.className, ComponentDescriber::paymentRole) != role) return null
  val resource = describers.representedClass(expression) ?: return null
  val count = (scalar as? ActualScalar)?.value ?: return null
  return ResourceAmount(
      count,
      describers.plainGainNoun(resource.className, count) ?: return null,
  )
}
