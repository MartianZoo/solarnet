package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.tfm.data.CardDefinition

internal fun renderActions(
    actions: List<Action>,
    card: CardDefinition? = null,
    describers: Describers,
): String? {
  if (actions.isEmpty()) return ""
  val rendered = actions.map { renderAction(it, card, describers) ?: return null }
  if (rendered.size == 1) return rendered.single().asSentences()
  val alternatives = rendered.map { it.asAlternative() ?: return null }
  return completeSentence(englishAlternatives(alternatives))
}

private fun renderAction(
    action: Action,
    card: CardDefinition?,
    describers: Describers,
): RenderedAction? {
  val cost = action.cost?.let { describers.renderCost(it) ?: return null }
  val result = renderInstructions(action.instruction, card, describers) ?: return null
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
      cost?.let { "$it to ${result.asCoordinatedClause()}" } ?: result.clauses.singleOrNull()
}
