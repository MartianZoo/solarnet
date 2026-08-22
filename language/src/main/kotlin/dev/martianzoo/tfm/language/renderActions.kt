package dev.martianzoo.tfm.language

import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.Dependency.Key

internal fun renderActions(
    actions: List<Action>,
    describers: Describers,
    drawFilter: EnglishDrawFilter? = null,
): Rendering<String> {
  if (actions.isEmpty()) return Rendering.resolved("")
  val rendered = actions.map { action ->
    renderAction(action, describers, drawFilter)
        ?: return Rendering.unresolved(
            action,
            actionRefusalReason(action, describers),
            completeSentence("[${actions.joinToString(" OR ")}]"),
        )
  }
  val unresolved = rendered.flatMap(RenderedAction::unresolved)
  if (rendered.size == 1) return Rendering(rendered.single().asSentences(), unresolved)
  val alternatives = rendered.mapIndexed { index, action ->
    action.asAlternative()
        ?: return Rendering(
            completeSentence("[${actions.joinToString(" OR ")}]"),
            unresolved +
                Unresolved(
                    actions[index],
                    RefusalReason.ACTION_ALTERNATIVES_NOT_COMBINABLE,
                ),
        )
  }
  val joined =
      if (alternatives.size == 2) alternatives.joinToString(", or ")
      else englishAlternatives(alternatives)
  return Rendering(completeSentence(joined), unresolved)
}

private fun actionRefusalReason(action: Action, describers: Describers): RefusalReason {
  val lowered = lowerProductionSyntax(action)
  val gatedCost = lowered.cost as? Cost.Gated
  if (
      (gatedCost?.cost ?: lowered.cost)?.let { describers.renderCost(it) } == null &&
          lowered.cost != null
  ) {
    return RefusalReason.UNSUPPORTED_ACTION_COST
  }
  if (gatedCost?.let { describers.renderGateCondition(it.gate) } == null && gatedCost != null) {
    return RefusalReason.UNSUPPORTED_ACTION_CONDITION
  }
  return RefusalReason.UNKNOWN_ACTION_FRAME
}

private fun Describers.renderCost(cost: Cost): Predicate? =
    when (cost) {
      is Cost.Or -> renderAlternativeCosts(cost.costs)
      is Cost.Spend -> renderSpendCost(cost)
      else -> null
    }

private fun Describers.renderAlternativeCosts(costs: Set<Cost>): Predicate? {
  val alternatives = costs.map { renderCost(it) ?: return null }
  return coordinatePredicateObjects(alternatives, Conjunction.OR)
}

private fun Describers.renderSpendCost(spend: Cost.Spend): Predicate? {
  val expression = spend.scaledEx.expression
  val count = spend.scaledEx.scalar.fixedQuantity() ?: return null
  renderResourceSpend(expression) { it.copy(count = count) }
      ?.let {
        return it
      }
  productionExpression(expression)?.let { production ->
    if (production.owner != null) return null
    val steps = if (count == 1) "step" else "steps"
    return Predicate(
        "decrease",
        Coordination.one(
            NounPhrase.text(
                "your ${componentNoun(production.resource, 1)} production $count $steps"
            )
        ),
    )
  }
  return null
}

private fun Describers.renderResourceSpend(
    expression: Expression,
    quantity: (NounPhrase) -> NounPhrase,
): Predicate? {
  if (expression.refinement == null && !expression.complement) {
    cardResourceNounPhrase(expression.className, 1)?.let { noun ->
      val resolved = resolveCardResource(expression) ?: return null
      val holder =
          when {
            cardResourceHasHolder(resolved, thisExpression) -> "this card"
            resolved.hasOnlySourceDependency(Key(OWNED, 0), anyoneExpression) -> "any player's card"
            resolved.sourceDependencies.isEmpty() -> "any of your cards"
            else -> return null
          }
      return Predicate(
          "remove",
          Coordination.one(quantity(noun.copy(count = null))),
          listOf(Modifier.Phrase("from $holder")),
      )
    }
  }
  if (!expression.simple || plainGainNoun(expression.className, 1) == null) return null
  val noun = componentNounPhrase(expression.className, 1).copy(count = null)
  return Predicate("spend", Coordination.one(quantity(noun)))
}

private fun Describers.renderLinkedXAction(action: Action): RenderedAction? {
  val spend = action.cost as? Cost.Spend ?: return null
  val costScalar = spend.scaledEx.scalar.variableQuantity() ?: return null
  val gain = action.instruction as? Gain ?: return null
  if (gain.intensity.modality() != Modality.REQUIRED) return null
  val gainScalar = gain.count.variableQuantity() ?: return null
  val gaining = gain.gaining
  if (!gaining.simple || !concrete(gaining.className) || !isStandardResource(gaining.className)) {
    return null
  }
  val cost =
      renderResourceSpend(spend.scaledEx.expression) { noun ->
        val quantity =
            if (costScalar.multiple == 1) "one or more ${noun.plural}"
            else "$costScalar ${noun.plural}"
        NounPhrase.text(quantity)
      } ?: return null
  val noun = fact(gaining.className, ComponentDescriber::noun)
  val resultQuantity =
      when {
        costScalar.multiple > 1 -> "$gainScalar ${componentNoun(gaining.className, 2)}"
        gainScalar.multiple == 1 && noun is ComponentDescriber.Noun.Fixed ->
            "that amount of ${componentNoun(gaining.className, 1)}"
        gainScalar.multiple == 1 -> "the same number of ${componentNoun(gaining.className, 2)}"
        gainScalar.multiple == 3 && noun is ComponentDescriber.Noun.Fixed ->
            "triple that amount of ${componentNoun(gaining.className, 1)}"
        else -> return null
      }
  val result =
      RenderedInstructions(
          listOf(
              Clause.Simple(Predicate("gain", Coordination.one(NounPhrase.text(resultQuantity))))
          )
      )
  return RenderedAction(cost, result)
}

private fun Describers.renderLinkedProductionResourceAction(action: Action): RenderedAction? {
  val spend = action.cost as? Cost.Spend ?: return null
  val costCount = spend.scaledEx.scalar.fixedQuantity() ?: return null
  val production = productionCategoryExpression(spend.scaledEx.expression) ?: return null
  if (production.owner != null || concrete(production.resource)) return null
  val gain = action.instruction as? Gain ?: return null
  if (gain.intensity.modality() != Modality.REQUIRED) return null
  if (!gain.gaining.simple || gain.gaining.className != production.resource) return null
  val gainCount = gain.count.fixedQuantity() ?: return null
  val steps = if (costCount == 1) "step" else "steps"
  val resources = if (gainCount == 1) "resource" else "resources"
  val cost =
      Predicate(
          "decrease",
          Coordination.one(NounPhrase.text("one of your productions $costCount $steps")),
      )
  val result =
      RenderedInstructions(
          listOf(
              Clause.Simple(
                  Predicate(
                      "gain",
                      Coordination.one(NounPhrase.text("$gainCount $resources of that kind")),
                  )
              )
          )
      )
  return RenderedAction(cost, result)
}

private fun Describers.renderDeferredPaymentAction(
    action: Action,
    drawFilter: EnglishDrawFilter?,
): RenderedAction? {
  if (action.cost != null) return null
  val sequence = action.instruction as? Then ?: return null
  if (sequence.stages.size != 2) return null

  val owed =
      paymentResourceGain(sequence.stages.first(), ComponentDescriber.PaymentRole.OWED, this)
          ?: return null
  val acceptance =
      paymentResourceGain(
          sequence.stages.last(),
          ComponentDescriber.PaymentRole.ACCEPTANCE,
          this,
      ) ?: return null
  if (acceptance.count != 1) return null

  val gated = sequence.continuation as? Gated ?: return null
  val barrier = gated.gate as? Requirement.Max ?: return null
  val barrierMetric = barrier.countedMetric as? Metric.Count ?: return null
  if (
      barrier.maximum != 0 ||
          !barrierMetric.expression.simple ||
          fact(barrierMetric.expression.className, ComponentDescriber::paymentRole) !=
              ComponentDescriber.PaymentRole.BARRIER
  ) {
    return null
  }
  val result = renderInstructions(gated.inner, this, drawFilter)
  val cost =
      Predicate(
          "spend",
          Coordination.one(NounPhrase.text("${owed.count} ${owed.noun}")),
          listOf(Modifier.Parenthetical("${acceptance.noun} may be used")),
      )
  return RenderedAction(cost, result)
}

private fun renderAction(
    action: Action,
    describers: Describers,
    drawFilter: EnglishDrawFilter?,
): RenderedAction? {
  val lowered = lowerProductionSyntax(action)
  describers.renderLinkedXAction(lowered)?.let {
    return it
  }
  describers.renderLinkedProductionResourceAction(lowered)?.let {
    return it
  }
  describers.renderDeferredPaymentAction(lowered, drawFilter)?.let {
    return it
  }
  val gatedCost = lowered.cost as? Cost.Gated
  val cost = (gatedCost?.cost ?: lowered.cost)?.let { describers.renderCost(it) ?: return null }
  val condition = gatedCost?.gate?.let { describers.renderGateCondition(it) ?: return null }
  val result = renderInstructions(lowered.instruction, describers, drawFilter)
  return RenderedAction(cost, result, condition)
}

private data class RenderedAction(
    val cost: Predicate?,
    val result: RenderedInstructions,
    val condition: String? = null,
) {
  val unresolved: List<Unresolved>
    get() = result.unresolved

  fun asSentences(): String {
    if (condition == null) {
      return cost?.let { completeSentence("${it.linearize()} to ${result.asCoordinatedClause()}") }
          ?: result.asSentences()
    }
    val clause =
        cost?.let { "${it.linearize()} to ${result.asCoordinatedClause()}" }
            ?: result.asCoordinatedClause()
    return completeSentence("$condition, $clause")
  }

  fun asAlternative(): String? {
    val clause =
        cost?.let { "${it.linearize()} to ${result.asCoordinatedClause()}" }
            ?: result.clauses.singleOrNull()?.linearize()
            ?: return null
    return condition?.let { "$it, $clause" } ?: clause
  }
}
