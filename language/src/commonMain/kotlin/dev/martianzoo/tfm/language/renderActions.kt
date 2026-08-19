package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.data.CardDefinition

internal fun renderActions(
    actions: List<Action>,
    card: CardDefinition? = null,
): String? {
  if (actions.isEmpty()) return ""
  val rendered = actions.map { renderAction(it, card) ?: return null }
  if (rendered.size == 1) return rendered.single()
  if (rendered.any { ". " in it.removeSuffix(".") }) return null
  return rendered
      .mapIndexed { index, action ->
        action.removeSuffix(".").let {
          if (index == 0) it else it.replaceFirstChar(Char::lowercaseChar)
        }
      }
      .joinToString(" or ", postfix = ".")
}

private fun renderAction(
    action: Action,
    card: CardDefinition?,
): String? {
  val result = renderInstructionClauses(action.instruction, card) ?: return null
  val cost =
      action.cost
          ?: return result.joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) + "." }
  val spending = renderCost(cost) ?: return null
  return "$spending to ${result.joinToString(" and ")}."
}

private fun renderCost(cost: Cost): String? {
  val spend = cost as? Cost.Spend ?: return null
  val expression = spend.scaledEx.expression
  if (!expression.simple || !isStandardResource(expression.className)) return null
  val count = (spend.scaledEx.scalar as? ActualScalar)?.value ?: return null
  return "Spend $count ${componentNoun(expression.className, count)}"
}
