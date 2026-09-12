package dev.martianzoo.tfm.text

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
    private val singularNoun: String,
    private val pluralNoun: String,
    val resource: ClassName?,
) {
  val noun: String
    get() = if (count == 1) singularNoun else pluralNoun

  val phrase: NounPhrase
    get() = NounPhrase(singularNoun, pluralNoun, count)

  fun withNoun(noun: ComponentDescriber.Noun.Counted): ResourceAmount =
      copy(singularNoun = noun.singular, pluralNoun = noun.plural)
}

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
  if (describers.resolvedRemovalModality(removal) != Modality.BEST_EFFORT) return null
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
  if (expression.refinement != null) return null
  if (describers.fact(expression.className, ComponentDescriber::paymentRole) != role) return null
  val count = scalar.fixedQuantity() ?: return null
  val represented = describers.representedClass(expression)
  val nouns =
      if (represented != null) {
        val singular = describers.plainGainNoun(represented.className, 1) ?: return null
        val plural = describers.plainGainNoun(represented.className, 2) ?: return null
        singular to plural
      } else {
        if (describers.resolveExpression(expression)?.sourceDependencies?.isNotEmpty() != false) {
          return null
        }
        val implicit =
            describers.fact(expression.className, ComponentDescriber::implicitPaymentResource)
                ?: return null
        describers.describedNoun(expression.className, implicit, 1) to
            describers.describedNoun(expression.className, implicit, 2)
      }
  return ResourceAmount(count, nouns.first, nouns.second, represented?.className)
}
