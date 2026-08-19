package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.InstructionTree

internal fun renderEffect(effect: Effect, describers: Describers): String? =
    if (isEndEffect(effect, describers)) {
      renderEndEffect(effect, describers)
    } else {
      paymentDiscount(effect, describers)?.let { renderPaymentDiscount(listOf(it)) }
          ?: renderResourcePaymentValue(effect, describers)
          ?: renderTriggeredInstructions(effect, describers)
    }

internal fun renderEffects(effects: List<Effect>, describers: Describers): String? {
  val sentences = mutableListOf<String>()
  var index = 0
  while (index < effects.size) {
    val discount = paymentDiscount(effects[index], describers)
    if (discount == null) {
      sentences += renderEffect(effects[index], describers) ?: return null
      index++
      continue
    }
    val run =
        effects
            .drop(index)
            .map { paymentDiscount(it, describers) }
            .takeWhile {
              it?.reduction == discount.reduction
            }
    sentences += renderPaymentDiscount(run.filterNotNull())
    index += run.size
  }
  return sentences.joinToString(" ")
}

private fun paymentDiscount(effect: Effect, describers: Describers): PaymentDiscount? {
  describers.renderOwedReduction(effect.instruction)?.let { reduction ->
    val trigger = describers.renderEventTrigger(effect.trigger) ?: return null
    return PaymentDiscount(
        trigger.removePrefix("when "),
        reduction,
        describers.paymentDiscountRefersToPlayedObject(effect.trigger),
    )
  }
  val trigger = describers.renderActionRefundDiscountTrigger(effect.trigger) ?: return null
  val reduction = describers.renderStandardResourceGainAmount(effect.instruction) ?: return null
  return PaymentDiscount(trigger, reduction, refersToObject = true)
}

private fun renderPaymentDiscount(discounts: List<PaymentDiscount>): String {
  val clauses = discounts.map { it.triggerClause }
  fun joinAlternatives(parts: List<String>): String =
      if (parts.size == 1) parts.single() else englishAlternatives(parts)
  val trigger =
      if (clauses.all { it.startsWith("you ") }) {
        "you " + joinAlternatives(clauses.map { it.removePrefix("you ") })
      } else {
        joinAlternatives(clauses)
      }
  val reduction = discounts.first().reduction
  val objectReference = if (discounts.all { it.refersToObject }) " for it" else ""
  return completeSentence(
      "when $trigger, you pay ${reduction.count} ${reduction.noun} less$objectReference"
  )
}

private data class PaymentDiscount(
    val triggerClause: String,
    val reduction: Describers.ResourceAmount,
    val refersToObject: Boolean,
)

private fun renderResourcePaymentValue(effect: Effect, describers: Describers): String? {
  val spent = describers.renderSpentResource(effect.trigger) ?: return null
  val reduction = describers.renderOwedReduction(effect.instruction) ?: return null
  return "Each $spent you spend is worth ${reduction.count} ${reduction.noun} extra."
}

private fun renderTriggeredInstructions(effect: Effect, describers: Describers): String? {
  val trigger = describers.renderEventTrigger(effect.trigger) ?: return null
  val result = renderInstructions(effect.instruction, describers = describers) ?: return null
  return completeSentence("$trigger, ${result.asCoordinatedClause()}")
}

internal fun renderEndEffect(effect: Effect, describers: Describers): String? {
  val condition =
      when (val trigger = effect.trigger) {
        is IfTrigger -> {
          if (!isEndTrigger(trigger.inner, describers)) return null
          describers.renderScoringCondition(trigger.condition) ?: return null
        }
        else -> {
          if (!isEndTrigger(trigger, describers)) return null
          null
        }
      }
  renderPerVictoryPoints(effect.instruction, describers)?.let {
    if (condition != null) return null
    return it
  }
  val points = describers.renderFixedScore(effect.instruction) ?: return null
  return "$points${condition?.let { " $it" } ?: ""}."
}

internal fun isEndEffect(effect: Effect, describers: Describers): Boolean {
  return isEndTrigger(effect.trigger, describers)
}

private fun isEndTrigger(trigger: Trigger, describers: Describers): Boolean =
    when (trigger) {
      is OnGainOf -> describers.isEndTrigger(trigger.expression)
      is Trigger.Or -> trigger.triggers.all { isEndTrigger(it, describers) }
      is Trigger.WrappingTrigger -> isEndTrigger(trigger.inner, describers)
      is Trigger.OnRemoveOf,
      Trigger.WhenGain,
      Trigger.WhenRemove -> false
    }

private fun renderPerVictoryPoints(
    instruction: InstructionTree,
    describers: Describers,
): String? {
  val per = instruction as? Per ?: return null
  val points = describers.renderFixedScore(per.inner) ?: return null
  val metric = renderMetricPhrase(per.metric, describers) ?: return null
  return "$points for $metric."
}
