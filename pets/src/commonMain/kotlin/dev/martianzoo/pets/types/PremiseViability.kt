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
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Or as InstructionOr
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.ast.Requirement

/** Exact premise checks whose proofs depend only on uninhabited Types. */
internal object PremiseViability {
  fun validate(table: ClassTable, selectedClassNames: Set<ClassName>) {
    selectedClassNames.forEach { className ->
      val declaration = table.getClass(className).declaration
      (declaration.properties[REQUIREMENT_PROPERTY] as? RequirementValue)?.let { property ->
        if (truthOf(property.value, table) == Truth.FALSE) {
          unviable(className, "impossible requirement ${property.value}")
        }
      }
      declaration.effects
          .filter { triggerReachable(it.trigger, table) }
          .forEach { effect ->
            impossibleRemoval(effect.instruction, table)?.let { removal ->
              unviable(className, "reachable mandatory removal $removal")
            }
          }
    }
  }

  private fun unviable(className: ClassName, reason: String): Nothing =
      throw IllegalArgumentException("unviable game premise: $className has $reason")

  private val REQUIREMENT_PROPERTY = PropertyName("requirement")

  private fun impossibleRemoval(tree: InstructionTree, table: ClassTable): Expression? =
      when (tree) {
        is Gated ->
            if (truthOf(tree.gate, table) == Truth.FALSE) null
            else impossibleRemoval(tree.inner, table)
        is InstructionOr ->
            tree.instructions
                .map { impossibleRemoval(it, table) }
                .takeIf { results -> results.all { it != null } }
                ?.first()
        is Per ->
            if (metricIsExactlyZero(tree.metric, table)) null
            else impossibleRemoval(tree.inner, table)
        is Change ->
            tree.removing?.takeIf {
              isUninhabited(it, table) &&
                  (tree.intensity ?: table.getClass(it.className).defaults.removeOnly.intensity) ==
                      MANDATORY
            }
        else ->
            tree.immediateChildren().filterIsInstance<InstructionTree>().firstNotNullOfOrNull {
              impossibleRemoval(it, table)
            }
      }

  private fun metricIsExactlyZero(metric: Metric, table: ClassTable): Boolean =
      metric is Count && isUninhabited(metric.expression, table)

  private fun triggerReachable(trigger: Trigger, table: ClassTable): Boolean =
      when (trigger) {
        is SelfTrigger -> true
        is OnGainOf -> !isUninhabited(trigger.expression, table)
        is OnRemoveOf -> !isUninhabited(trigger.expression, table)
        is Or -> trigger.triggers.any { triggerReachable(it, table) }
        is ByTrigger -> triggerReachable(trigger.inner, table) && !isUninhabited(trigger.by, table)
        is IfTrigger ->
            triggerReachable(trigger.inner, table) &&
                truthOf(trigger.condition, table) != Truth.FALSE
        is XTrigger -> triggerReachable(trigger.inner, table)
        is Transform -> triggerReachable(trigger.inner, table)
      }

  private fun truthOf(requirement: Requirement, table: ClassTable): Truth =
      when (requirement) {
        is Requirement.Counting -> {
          val metric = requirement.metric
          if (metric is Count && isUninhabited(metric.expression, table)) {
            if (0 in requirement.range) Truth.TRUE else Truth.FALSE
          } else {
            Truth.UNKNOWN
          }
        }
        is Requirement.And -> truthOfAll(requirement.requirements.map { truthOf(it, table) })
        is Requirement.Or -> truthOfAny(requirement.requirements.map { truthOf(it, table) })
        is Requirement.Eval,
        is Requirement.Transform -> Truth.UNKNOWN
      }

  private fun isUninhabited(expression: Expression, table: ClassTable): Boolean {
    if (expression.className == THIS) return false
    if (!expression.complement && !table.isActive(expression.className)) return true
    return expression.arguments.any { isUninhabited(it, table) }
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
