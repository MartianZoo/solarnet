package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal fun renderActions(
    actions: List<Action>,
    describers: Describers,
): String? {
  if (actions.isEmpty()) return ""
  val rendered = actions.map { renderAction(it, describers) ?: return null }
  if (rendered.size == 1) return rendered.single().asSentences()
  val alternatives = rendered.map { it.asAlternative() ?: return null }
  val joined =
      if (alternatives.size == 2) alternatives.joinToString(", or ")
      else englishAlternatives(alternatives)
  return completeSentence(joined)
}

private fun Describers.renderCost(cost: Cost): String? {
  val spend = cost as? Cost.Spend ?: return null
  val expression = spend.scaledEx.expression
  val count = (spend.scaledEx.scalar as? ActualScalar)?.value ?: return null
  if (expression.refinement == null && !expression.complement) {
    cardResourceNoun(expression.className, count)?.let { noun ->
      val holder =
          when (expression.arguments) {
            listOf(thisExpression) -> "this card"
            listOf(anyoneExpression) -> "ANY PLAYER'S CARD"
            emptyList<Expression>() -> "any of your cards"
            else -> return null
          }
      return "remove $count $noun from $holder"
    }
  }
  productionExpression(expression)?.let { (ownerArguments, resourceClassName) ->
    if (ownerArguments.isNotEmpty()) return null
    val steps = if (count == 1) "step" else "steps"
    return "decrease your ${componentNoun(resourceClassName, 1)} production $count $steps"
  }
  if (!expression.simple) return null
  val noun = plainGainNoun(expression.className, count) ?: return null
  return "spend $count $noun"
}

private fun renderAction(
    action: Action,
    describers: Describers,
): RenderedAction? {
  val lowered = lowerProductionSyntax(action)
  val cost = lowered.cost?.let { describers.renderCost(it) ?: return null }
  val result = renderInstructions(lowered.instruction, describers) ?: return null
  return RenderedAction(cost, result)
}

private data class RenderedAction(
    val cost: String?,
    val result: RenderedInstructions,
) {
  fun asSentences(): String =
      cost?.let { completeSentence("$it to ${result.asCoordinatedClause()}") }
          ?: result.asSentences()

  fun asAlternative(): String? =
      cost?.let { "$it to ${result.asCoordinatedClause()}" }
          ?: result.clauses.singleOrNull()?.linearize()
}
