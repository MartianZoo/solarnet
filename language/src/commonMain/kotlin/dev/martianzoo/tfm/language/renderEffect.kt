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
      renderPaymentDiscount(effect, describers)
          ?: renderResourcePaymentValue(effect, describers)
          ?: renderTriggeredInstructions(effect, describers)
    }

private fun renderPaymentDiscount(effect: Effect, describers: Describers): String? {
  val trigger = describers.renderEventTrigger(effect.trigger) ?: return null
  val reduction = describers.renderOwedReduction(effect.instruction) ?: return null
  return "${trigger.replaceFirstChar(Char::uppercaseChar)}, you pay ${reduction.count} ${reduction.noun} less for it."
}

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
