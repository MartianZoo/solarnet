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

/**
 * Proves facts that follow only from exact counts and uninhabited expression domains. [exactCount]
 * returns null when a count is unknown. An unproved fact remains unknown, so callers can reject
 * impossible behavior without rejecting behavior that may be possible.
 */
public class InhabitanceInterpreter(
    private val classIsUninhabited: (ClassName) -> Boolean,
    private val exactCount: (Expression) -> Int? = { null },
) {
  internal sealed interface SimplifiedRequirement {
    data object Always : SimplifiedRequirement

    data object Never : SimplifiedRequirement

    data class Conditional(val requirement: Requirement) : SimplifiedRequirement
  }

  /** Whether the expression is provably empty from its root, arguments, or requirements. */
  public fun expressionIsUninhabited(expression: Expression): Boolean {
    if (expression.className == THIS) return false
    if (classIsUninhabited(expression.className)) return true
    if (expression.arguments.any(::expressionIsUninhabited)) return true
    return expression.refinement?.conjuncts()?.filterIsInstance<Has>()?.any {
      requirementIsFalse(it.requirement)
    } == true
  }

  /** Whether known counts and empty domains prove the requirement false. */
  public fun requirementIsFalse(requirement: Requirement): Boolean =
      simplifyRequirement(requirement) == SimplifiedRequirement.Never

  /** Removes every fixed part of [requirement], retaining only its state-dependent question. */
  internal fun simplifyRequirement(requirement: Requirement): SimplifiedRequirement =
      when (requirement) {
        is Requirement.Counting -> {
          val metric = requirement.metric
          val count =
              (metric as? Count)?.let { exactCount(it.expression) }
                  ?: 0.takeIf { metricIsExactlyZero(metric) }
          when {
            count == null -> SimplifiedRequirement.Conditional(requirement)
            count in requirement.range -> SimplifiedRequirement.Always
            else -> SimplifiedRequirement.Never
          }
        }
        is Requirement.And -> {
          val simplified = requirement.requirements.map(::simplifyRequirement)
          when {
            SimplifiedRequirement.Never in simplified -> SimplifiedRequirement.Never
            else -> {
              val remaining = simplified.filterIsInstance<SimplifiedRequirement.Conditional>()
              if (remaining.isEmpty()) {
                SimplifiedRequirement.Always
              } else {
                val parts = remaining.map { it.requirement }
                val combined = Requirement.And.create(parts)
                if (parts.none { it === combined }) {
                  combined.sourceLocation = requirement.sourceLocation
                }
                SimplifiedRequirement.Conditional(combined)
              }
            }
          }
        }
        is Requirement.Or -> {
          val simplified = requirement.requirements.map(::simplifyRequirement)
          when {
            SimplifiedRequirement.Always in simplified -> SimplifiedRequirement.Always
            else -> {
              val remaining = simplified.filterIsInstance<SimplifiedRequirement.Conditional>()
              if (remaining.isEmpty()) {
                SimplifiedRequirement.Never
              } else {
                val parts = remaining.map { it.requirement }
                val combined = Requirement.Or.create(parts)
                if (parts.none { it === combined }) {
                  combined.sourceLocation = requirement.sourceLocation
                }
                SimplifiedRequirement.Conditional(combined)
              }
            }
          }
        }
        is Requirement.Eval,
        is Requirement.Transform -> SimplifiedRequirement.Conditional(requirement)
      }

  /** Whether the metric is provably zero; false includes unknown values. */
  public fun metricIsExactlyZero(metric: Metric): Boolean =
      when (metric) {
        is Count -> exactCount(metric.expression) == 0 || expressionIsUninhabited(metric.expression)
        is Metric.Or -> metric.metrics.all(::metricIsExactlyZero)
        else -> false
      }

  /** Whether the trigger may be reachable; false means it is provably unreachable. */
  public fun triggerIsReachable(trigger: Trigger): Boolean =
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
}
