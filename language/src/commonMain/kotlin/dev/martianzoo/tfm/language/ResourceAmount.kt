package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

/** A concrete standard-resource amount retained across payment constructions. */
internal data class ResourceAmount(val count: Int, val noun: String)

internal fun owedReduction(
    instruction: InstructionTree,
    describers: Describers,
): ResourceAmount? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity != null && removal.intensity != MANDATORY) return null
  val expression = removal.removing
  if (expression.refinement != null || expression.complement) return null
  if (describers.fact(expression.className, ComponentDescriber::owedPayment) != true) return null
  val resource = describers.representedClass(expression) ?: return null
  val count = (removal.count as? ActualScalar)?.value ?: return null
  return ResourceAmount(
      count,
      describers.plainGainNoun(resource.className, count) ?: return null,
  )
}
