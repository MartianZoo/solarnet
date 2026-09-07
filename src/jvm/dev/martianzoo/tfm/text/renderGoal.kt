package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.MetricValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.pets.types.Class

/** Selects the shared structured rendering owned by each goal's declared semantic property. */
internal fun renderGoal(goal: Class, describers: Describers): EnglishGoalRendering {
  val rendered =
      when {
        goal.isSubtypeOf(goal.classTable.getClass(MILESTONE)) -> {
          val requirement = (goal.properties.getValue(REQUIREMENT) as RequirementValue).value
          renderRequirement(
              replaceThisExpressionsWith(goal.className.expression)
                  .transformRequirement(requirement),
              describers,
          )
        }
        goal.isSubtypeOf(goal.classTable.getClass(AWARD)) -> {
          val metric = (goal.properties.getValue(METRIC) as MetricValue).value
          val contextual =
              replaceThisExpressionsWith(goal.className.expression).transformMetric(metric)
          val phrase =
              renderRankedMetricPhrase(describers.lowerProductionSyntax(contextual), describers)
          phrase?.let(::Sentence)?.render()
              ?: Rendering.unresolved(
                  metric,
                  RefusalReason.UNSUPPORTED_METRIC,
                  completeSentence("[$metric]"),
              )
        }
        else -> error("${goal.className} is not a milestone or award")
      }
  return EnglishGoalRendering(rendered.value, rendered.unresolved)
}

private val MILESTONE = cn("Milestone")
private val AWARD = cn("Award")
private val REQUIREMENT = PropertyName("requirement")
private val METRIC = PropertyName("metric")
