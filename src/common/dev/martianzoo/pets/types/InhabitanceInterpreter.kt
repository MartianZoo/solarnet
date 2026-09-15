package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Effect.Trigger.Or
import dev.martianzoo.pets.ast.Effect.Trigger.SelfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.Transform
import dev.martianzoo.pets.ast.Effect.Trigger.XTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Has
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.Requirement

/** Proves facts that follow only from exact counts and uninhabited expression domains. */
internal class InhabitanceInterpreter(
    private val classIsUninhabited: (ClassName) -> Boolean,
    private val exactCount: (Expression) -> Int? = { null },
) {
  internal fun expressionIsUninhabited(expression: Expression): Boolean {
    if (expression.className == THIS) return false
    if (classIsUninhabited(expression.className)) return true
    if (expression.arguments.any(::expressionIsUninhabited)) return true
    return expression.refinement?.conjuncts()?.filterIsInstance<Has>()?.any {
      requirementIsFalse(it.requirement)
    } == true
  }

  internal fun requirementIsFalse(requirement: Requirement): Boolean =
      truthOf(requirement) == Truth.FALSE

  internal fun metricIsExactlyZero(metric: Metric): Boolean =
      when (metric) {
        is Count -> exactCount(metric.expression) == 0 || expressionIsUninhabited(metric.expression)
        is Metric.Or -> metric.metrics.all(::metricIsExactlyZero)
        else -> false
      }

  internal fun triggerIsReachable(trigger: Trigger): Boolean =
      when (trigger) {
        is SelfTrigger -> true
        is OnGainOf -> !expressionIsUninhabited(trigger.expression)
        is OnRemoveOf -> !expressionIsUninhabited(trigger.expression)
        is Or -> trigger.triggers.any(::triggerIsReachable)
        is ByTrigger -> triggerIsReachable(trigger.inner) && !expressionIsUninhabited(trigger.by)
        is IfTrigger -> triggerIsReachable(trigger.inner) && !requirementIsFalse(trigger.condition)
        is XTrigger -> triggerIsReachable(trigger.inner)
        is Transform -> triggerIsReachable(trigger.inner)
      }

  private fun truthOf(requirement: Requirement): Truth =
      when (requirement) {
        is Requirement.Counting -> {
          val metric = requirement.metric
          val count =
              (metric as? Count)?.let { exactCount(it.expression) }
                  ?: 0.takeIf { metricIsExactlyZero(metric) }
          when {
            count == null -> Truth.UNKNOWN
            count in requirement.range -> Truth.TRUE
            else -> Truth.FALSE
          }
        }
        is Requirement.And -> truthOfAll(requirement.requirements.map(::truthOf))
        is Requirement.Or -> truthOfAny(requirement.requirements.map(::truthOf))
        is Requirement.Eval,
        is Requirement.Transform -> Truth.UNKNOWN
      }

  private fun truthOfAll(values: Collection<Truth>): Truth =
      when {
        Truth.FALSE in values -> Truth.FALSE
        values.all { it == Truth.TRUE } -> Truth.TRUE
        else -> Truth.UNKNOWN
      }

  private fun truthOfAny(values: Collection<Truth>): Truth =
      when {
        Truth.TRUE in values -> Truth.TRUE
        values.all { it == Truth.FALSE } -> Truth.FALSE
        else -> Truth.UNKNOWN
      }

  private enum class Truth {
    TRUE,
    FALSE,
    UNKNOWN,
  }
}
