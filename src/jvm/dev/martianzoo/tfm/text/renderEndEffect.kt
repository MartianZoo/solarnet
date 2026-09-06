package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement

internal fun renderEndEffect(effect: Effect, describers: Describers): Rendering<String>? {
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
  return Rendering.resolved("$points${condition?.let { " $it" } ?: ""}.")
}

internal fun isEndEffect(effect: Effect, describers: Describers): Boolean =
    isEndTrigger(effect.trigger, describers)

internal fun isUnconditionalFixedScore(effect: Effect, describers: Describers): Boolean =
    effect.trigger !is IfTrigger &&
        isEndEffect(effect, describers) &&
        describers.renderFixedScore(effect.instruction) != null

private fun isEndTrigger(trigger: Trigger, describers: Describers): Boolean =
    when (trigger) {
      is OnGainOf -> describers.isEndTrigger(trigger.expression)
      is Trigger.Or -> trigger.triggers.all { isEndTrigger(it, describers) }
      is Trigger.WrappingTrigger -> isEndTrigger(trigger.inner, describers)
      is Trigger.OnRemoveOf,
      Trigger.WhenGain,
      Trigger.WhenRemove -> false
    }

private fun Describers.isEndTrigger(expression: Expression): Boolean =
    expression.simple && isEndTrigger(expression.className)

private fun Describers.renderScoringCondition(requirement: Requirement): String? {
  val minimum = requirement as? Requirement.Min ?: return null
  val metric = minimum.metric as? Metric.Count ?: return null
  val expression = metric.expression
  val resolved = resolveCardResource(expression) ?: return null
  if (
      !cardResourceHasHolder(resolved, thisExpression) ||
          expression.refinement != null ||
          expression.complement
  )
      return null
  val noun = cardResourceNoun(expression.className, maxOf(2, minimum.target)) ?: return null
  return "if you have ${minimum.target} or more $noun on this card"
}

private fun Describers.renderFixedScore(instruction: InstructionTree): String? {
  val (className, count, penalty) =
      when (instruction) {
        is Gain -> {
          if (instruction.intensity.modality() != Modality.REQUIRED) return null
          if (!instruction.gaining.simple) return null
          Triple(
              instruction.gaining.className,
              instruction.count.fixedQuantity() ?: return null,
              false,
          )
        }
        is Remove -> {
          if (instruction.intensity.modality() != Modality.REQUIRED) return null
          if (!instruction.removing.simple) return null
          Triple(
              instruction.removing.className,
              instruction.count.fixedQuantity() ?: return null,
              true,
          )
        }
        else -> return null
      }
  val score = fact(className, ComponentDescriber::score) ?: return null
  return "${if (penalty) "-" else ""}$count ${if (count == 1) score.singular else score.plural}"
}

private fun renderPerVictoryPoints(
    instruction: InstructionTree,
    describers: Describers,
): Rendering<String>? {
  val per = instruction as? Per ?: return null
  val points = describers.renderFixedScore(per.inner) ?: return null
  val metric = renderMetricPhrase(per.metric, describers)
  val text = "$points ${Modifier.Per(metric ?: NounPhrase.text("[${per.metric}]")).linearize()}."
  return if (metric != null) {
    Rendering.resolved(text)
  } else {
    Rendering.unresolved(per.metric, RefusalReason.UNSUPPORTED_METRIC, text)
  }
}
