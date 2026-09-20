package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.Transforming.actionSelectors
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.data.ClassDeclaration

/** Rewrites Terraforming Mars standard-resource action costs into its billing protocol. */
internal object TfmActionLowerer {
  fun lower(source: ClassDeclaration): ClassDeclaration {
    if (source.authoredActions.isEmpty()) return source
    val ordinaryEffects = actionListToEffects(source.authoredActions)
    val selectors = actionSelectors(source.authoredActions).toList()
    val loweredActions =
        source.authoredActions.withIndex().flatMap { (index0Ref, action) ->
          actionToEffects(action, selectors[index0Ref], ordinaryEffects[index0Ref])
        }
    val executableEffects = source.authoredEffects + loweredActions
    return source.copy(
        executableEffects =
            executableEffects.takeUnless { it == source.authoredEffectsWithActions },
    )
  }

  private fun actionToEffects(
      action: Action,
      selector: ClassName,
      ordinaryEffect: Effect,
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
    val billingResource =
        if (spend.scaledEx.expression.className == MC) ""
        else ", Class<${spend.scaledEx.expression}>"
    if (spend.scaledEx.scalar is XScalar) {
      return listOf(
          parse(
              "UseAction<This, $selector>: $owed THEN " +
                  "ActionBilling<This, $selector$billingResource> THEN " +
                  "MAX 0 ActionBilling: (${action.instruction})"
          )
      )
    }

    return listOf(
        parse(
            "UseAction<This, $selector>: $owed THEN " +
                "ActionBilling<This, $selector$billingResource>"
        ),
        parse("-ActionBilling<This, $selector>: " + action.instruction),
    )
  }

  private val MC: ClassName = cn("MC")

  private val standardResourceClasses: Set<ClassName> =
      setOf(MC, cn("Steel"), cn("Titanium"), cn("Plant"), cn("Energy"), cn("Heat"))
}
