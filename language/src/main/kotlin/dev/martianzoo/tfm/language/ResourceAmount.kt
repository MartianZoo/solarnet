package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.ScaledExpression.Scalar

/** A concrete standard-resource amount retained across payment constructions. */
internal data class ResourceAmount(
    val count: Int,
    val noun: String,
    val resource: ClassName?,
)

internal fun owedReduction(
    instruction: InstructionTree,
    describers: Describers,
): ResourceAmount? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity.modality() != Modality.REQUIRED) return null
  return paymentResourceAmount(
      removal.removing,
      removal.count,
      ComponentDescriber.PaymentRole.OWED,
      describers,
  )
}

internal fun maximumOwedReduction(
    instruction: InstructionTree,
    describers: Describers,
): ResourceAmount? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity.modality() != Modality.BEST_EFFORT) return null
  return paymentResourceAmount(
      removal.removing,
      removal.count,
      ComponentDescriber.PaymentRole.OWED,
      describers,
  )
}

internal fun completeOwedReduction(
    instruction: InstructionTree,
    describers: Describers,
): ResourceAmount? {
  val per = instruction as? Per ?: return null
  val removal = per.inner as? Remove ?: return null
  val counted = per.metric as? Metric.Count ?: return null
  if (removal.removing != counted.expression) return null
  if (removal.intensity.modality() != Modality.REQUIRED) return null
  return paymentResourceAmount(
          removal.removing,
          removal.count,
          ComponentDescriber.PaymentRole.OWED,
          describers,
      )
      ?.takeIf { it.count == 1 }
}

internal fun paymentResourceGain(
    instruction: InstructionTree,
    role: ComponentDescriber.PaymentRole,
    describers: Describers,
): ResourceAmount? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity.modality() != Modality.REQUIRED) return null
  return paymentResourceAmount(gain.gaining, gain.count, role, describers)
}

private fun paymentResourceAmount(
    expression: Expression,
    scalar: Scalar,
    role: ComponentDescriber.PaymentRole,
    describers: Describers,
): ResourceAmount? {
  if (expression.refinement != null || expression.complement) return null
  if (describers.fact(expression.className, ComponentDescriber::paymentRole) != role) return null
  val count = scalar.fixedQuantity() ?: return null
  val represented = describers.representedClass(expression)
  val noun =
      if (represented != null) {
        describers.plainGainNoun(represented.className, count) ?: return null
      } else {
        if (describers.resolveExpression(expression)?.sourceDependencies?.isNotEmpty() != false) {
          return null
        }
        val implicit =
            describers.fact(expression.className, ComponentDescriber::implicitPaymentResource)
                ?: return null
        describers.describedNoun(expression.className, implicit, count)
      }
  return ResourceAmount(count, noun, represented?.className)
}
