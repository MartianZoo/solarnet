package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.Transforming.actionSelectors
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.tfm.canon.TfmClasses.STANDARD_RESOURCE

/** Rewrites Terraforming Mars action costs and rejects its absent action slots. */
internal object TfmActionLowerer {
  fun lower(declarations: Collection<ClassDeclaration>): List<ClassDeclaration> {
    val standardResources = mutableSetOf(STANDARD_RESOURCE)
    do {
      val changed =
          declarations
              .filter { declaration ->
                declaration.supertypes.any { it.className in standardResources }
              }
              .map { it.className }
              .let(standardResources::addAll)
    } while (changed)
    return declarations.map { lower(it, standardResources) }
  }

  private fun lower(
      source: ClassDeclaration,
      standardResourceClasses: Set<ClassName>,
  ): ClassDeclaration {
    if (source.authoredActions.isEmpty()) return source
    val ordinaryEffects = actionListToEffects(source.authoredActions)
    val selectors = actionSelectors(source.authoredActions).toList()
    val loweredActions =
        source.authoredActions.withIndex().flatMap { (index0Ref, action) ->
          actionToEffects(
              action,
              selectors[index0Ref],
              ordinaryEffects[index0Ref],
              standardResourceClasses,
          )
        }
    val absentActions =
        (source.authoredActions.size + 1..3).map {
          parse<Effect>("UseAction<This, Action$it>: Die")
        }
    val executableEffects = source.authoredEffects + loweredActions + absentActions
    return source.copy(
        executableEffects =
            executableEffects.takeUnless { it == source.authoredEffectsWithActions },
    )
  }

  private fun actionToEffects(
      action: Action,
      selector: ClassName,
      ordinaryEffect: Effect,
      standardResourceClasses: Set<ClassName>,
  ): List<Effect> {
    val (spend, metric) =
        when (val cost = action.cost) {
          is Action.Cost.Spend -> cost to null
          is Action.Cost.Per -> (cost.cost as? Action.Cost.Spend)?.let { it to cost.metric }
          else -> null
        } ?: return listOf(ordinaryEffect)
    if (spend.scaledEx.expression.className !in standardResourceClasses) {
      return listOf(ordinaryEffect)
    }

    val metricText =
        when (metric) {
          is Property -> if (metric.receiver == null) "This.$metric" else "$metric"
          else -> "$metric"
        }
    val owed =
        "${spend.scaledEx.scalar} Owed<Class<${spend.scaledEx.expression}>>" +
            if (metric == null) "" else " / $metricText"
    val paymentScope =
        "PaymentScope<Class<This>, Class<${spend.scaledEx.expression}>, Class<$selector>>"
    if (spend.scaledEx.scalar is XScalar) {
      return listOf(
          parse(
              "UseAction<This, $selector>: $owed THEN " +
                  "$paymentScope THEN " +
                  "MAX 0 PaymentScope: (${action.instruction})"
          )
      )
    }

    return listOf(
        parse("UseAction<This, $selector>: $owed THEN " + paymentScope),
        parse("-$paymentScope: " + action.instruction),
    )
  }
}
