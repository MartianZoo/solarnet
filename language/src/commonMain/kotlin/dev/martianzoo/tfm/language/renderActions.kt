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
  if (rendered.size == 1) return rendered.single().asSentences()
  val alternatives = rendered.map { it.asAlternative() ?: return null }
  return completeSentence(englishAlternatives(alternatives))
}

private fun renderAction(
    action: Action,
    card: CardDefinition?,
): RenderedAction? {
  val cost = action.cost?.let { renderCost(it) ?: return null }
  val result = renderInstructions(action.instruction, card) ?: return null
  return RenderedAction(cost, result)
}

private fun renderCost(cost: Cost): String? {
  val spend = cost as? Cost.Spend ?: return null
  val expression = spend.scaledEx.expression
  if (!expression.simple || !isStandardResource(expression.className)) return null
  val count = (spend.scaledEx.scalar as? ActualScalar)?.value ?: return null
  return "spend $count ${componentNoun(expression.className, count)}"
}

private data class RenderedAction(
    val cost: String?,
    val result: RenderedInstructions,
) {
  fun asSentences(): String =
      cost?.let { completeSentence("$it to ${result.asCoordinatedClause()}") }
          ?: result.asSentences()

  fun asAlternative(): String? =
      cost?.let { "$it to ${result.asCoordinatedClause()}" } ?: result.clauses.singleOrNull()
}
