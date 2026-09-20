package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.tfm.canon.CardOperation

internal fun renderActions(
    actions: List<Action>,
    describers: Describers,
    firstActionPaymentResource: String? = null,
): Rendering<String> {
  if (actions.isEmpty()) return Rendering.resolved("")
  val rendered = actions.mapIndexed { index, action ->
    renderAction(action, describers)?.let { renderedAction ->
      if (index == 0 && firstActionPaymentResource != null) {
        renderedAction.withCostModifier(
            Modifier.Parenthetical("$firstActionPaymentResource may be used")
        )
      } else {
        renderedAction
      }
    }
        ?: return Rendering.unresolved(
            action,
            actionRefusalReason(action, describers),
            completeSentence("[${actions.joinToString(" OR ")}]"),
        )
  }
  val unresolved = rendered.flatMap(RenderedAction::unresolved)
  if (rendered.size == 1) return rendered.single().asSentences()
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
  val conjunction = if (alternatives.size == 2) Conjunction.COMMA_OR else Conjunction.OR
  val joined = Clause.Coordinated(Coordination(alternatives, conjunction))
  return Sentence(joined).render()
}

private fun actionRefusalReason(action: Action, describers: Describers): RefusalReason {
  val lowered = describers.prepareForRendering(action)
  val gatedInstruction = lowered.instruction as? Gated
  if (lowered.cost?.let { describers.renderCost(it) } == null && lowered.cost != null) {
    return RefusalReason.UNSUPPORTED_ACTION_COST
  }
  if (
      gatedInstruction?.let { describers.renderGateCondition(it.gate) } == null &&
          gatedInstruction != null
  ) {
    return RefusalReason.UNSUPPORTED_ACTION_CONDITION
  }
  return RefusalReason.UNKNOWN_ACTION_FRAME
}

private fun Describers.renderCost(cost: Cost): Predicate? =
    when (cost) {
      is Cost.Spend -> renderSpendCost(cost)
      is Cost.Per -> renderReducedVariableCost(cost)?.first
      else -> null
    }

private fun Describers.renderReducedVariableCost(
    per: Cost.Per,
): Pair<Predicate, Clause.Simple>? {
  val spend = per.cost as? Cost.Spend ?: return null
  if (
      !spend.scaledEx.expression.simple || !isStandardResource(spend.scaledEx.expression.className)
  ) {
    return null
  }
  val unitCost = spend.scaledEx.scalar.fixedQuantity() ?: return null
  val reduction = per.metric as? Metric.Subtract ?: return null
  val maximum = (reduction.minuend as? Metric.Constant)?.value ?: return null
  val metric = renderMetricPhrase(reduction.subtrahend, this) ?: return null
  val payment =
      renderResourceSpend(spend.scaledEx.expression) { noun ->
        noun.copy(count = unitCost * maximum)
      } ?: return null
  val reductionAmount =
      componentNounPhrase(spend.scaledEx.expression.className, unitCost)
          .withModifier(Modifier.Per(metric))
  val explanation =
      Clause.Simple(
          subject = NounPhrase.text("this cost"),
          predicate =
              Predicate(
                  Verb("is reduced"),
                  modifiers = listOf(Modifier.Relation("by", reductionAmount)),
              ),
      )
  return payment to explanation
}

private fun Describers.renderSpendCost(spend: Cost.Spend): Predicate? {
  val expression = spend.scaledEx.expression
  val count = spend.scaledEx.scalar.fixedQuantity()
  if (count == null) {
    val quantity = spend.scaledEx.scalar.variableQuantity() ?: return null
    return renderResourceSpend(expression) { noun ->
      NounPhrase.text(
          if (quantity.multiple == 1) "1 or more ${noun.plural}" else "$quantity ${noun.plural}"
      )
    }
  }
  renderResourceSpend(expression) { it.copy(count = count) }
      ?.let {
        return it
      }
  productionCategoryExpression(expression, this)?.let { production ->
    if (production.owner != null) return null
    val selectedProduction =
        if (concrete(production.resource)) {
          NounPhrase(
              productionNoun(production.resource),
              determiner = Determiner.YOUR,
          )
        } else {
          oneOfYour("productions")
        }
    return Predicate(
        Verb("decrease"),
        Coordination.one(selectedProduction.withModifier(Modifier.Phrase(stepCount(count)))),
    )
  }
  return null
}

private fun Describers.renderResourceSpend(
    expression: Expression,
    quantity: (NounPhrase) -> NounPhrase,
): Predicate? {
  if (expression.refinement == null) {
    cardResourceNounPhrase(expression.className, 1)?.let { noun ->
      val resolved = resolveCardResource(expression) ?: return null
      val (holder, verb) =
          when {
            cardResourceHasHolder(resolved, thisExpression) -> "this card" to "spend"
            resolved.hasOnlySourceDependency(Key(OWNED, 0), anyoneExpression) ->
                "any player's card" to "remove"
            resolved.sourceDependencies.isEmpty() -> "any of your cards" to "spend"
            else -> return null
          }
      return Predicate(
          Verb(verb),
          Coordination.one(quantity(noun.copy(count = null))),
          listOf(Modifier.Phrase("from $holder")),
      )
    }
    if (changeFrame(expression.className) == ComponentDescriber.ChangeFrame.Deck) {
      val cards = componentNounPhrase(expression.className, 1).copy(count = null)
      return Predicate(Verb("discard"), Coordination.one(quantity(cards)))
    }
  }
  if (!expression.simple || plainGainNoun(expression.className, 1) == null) return null
  val noun = componentNounPhrase(expression.className, 1).copy(count = null)
  return Predicate(Verb("spend"), Coordination.one(quantity(noun)))
}

private fun Describers.renderLinkedXAction(action: Action): RenderedAction? {
  val spend = action.cost as? Cost.Spend ?: return null
  val costScalar = spend.scaledEx.scalar.variableQuantity() ?: return null
  val gain = action.instruction as? Gain ?: return null
  if (gain.quantifier.modality() != Modality.REQUIRED) return null
  val gainScalar = gain.count.variableQuantity() ?: return null
  val gaining = gain.gaining
  if (!gaining.simple || !isStandardResource(gaining.className)) {
    return null
  }
  val cost =
      renderResourceSpend(spend.scaledEx.expression) { noun ->
        val quantity =
            if (costScalar.multiple == 1) "1 or more ${noun.plural}"
            else "$costScalar ${noun.plural}"
        NounPhrase.text(quantity)
      } ?: return null
  val noun = fact(gaining.className, ComponentDescriber::noun)
  val resultQuantity =
      when {
        costScalar.multiple > 1 -> "$gainScalar ${componentNoun(gaining.className, 2)}"
        gainScalar.multiple == 1 && !concrete(gaining.className) ->
            "the same number of one ${componentNoun(gaining.className, 1)}"
        gainScalar.multiple == 1 && noun is ComponentDescriber.Noun.Fixed ->
            "that amount of ${componentNoun(gaining.className, 1)}"
        gainScalar.multiple == 1 -> "the same number of ${componentNoun(gaining.className, 2)}"
        gainScalar.multiple == 2 && noun is ComponentDescriber.Noun.Fixed ->
            "twice that amount of ${componentNoun(gaining.className, 1)}"
        gainScalar.multiple == 3 && noun is ComponentDescriber.Noun.Fixed ->
            "triple that amount of ${componentNoun(gaining.className, 1)}"
        else -> return null
      }
  val result =
      RenderedInstructions(
          listOf(
              Clause.Simple(
                  Predicate(Verb("gain"), Coordination.one(NounPhrase.text(resultQuantity)))
              )
          )
      )
  return RenderedAction(cost, result)
}

private fun Describers.renderDeferredPaymentAction(
    action: Action,
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
  val result = renderInstructions(gated.inner, this)
  val cost =
      Predicate(
          Verb("spend"),
          Coordination.one(owed.phrase),
          listOf(Modifier.Parenthetical("${acceptance.noun} may be used")),
      )
  return RenderedAction(cost, result)
}

private fun renderAction(
    action: Action,
    describers: Describers,
): RenderedAction? {
  val lowered = describers.prepareForRendering(action)
  describers.renderLinkedXAction(lowered)?.let {
    return it.takeIf(RenderedAction::costCanJoinResult)
  }
  describers.renderDeferredPaymentAction(lowered)?.let {
    return it.takeIf(RenderedAction::costCanJoinResult)
  }
  val gatedInstruction = lowered.instruction as? Gated
  val reducedCost = (lowered.cost as? Cost.Per)?.let(describers::renderReducedVariableCost)
  val cost = reducedCost?.first ?: lowered.cost?.let { describers.renderCost(it) ?: return null }
  val condition =
      gatedInstruction?.gate?.let {
        describers.renderGateCondition(it) ?: return null
      }
  val result =
      renderPreparedInstructions(
          gatedInstruction?.inner ?: lowered.instruction,
          describers,
          TypeVariableReferences.from(lowered),
      )
  val separateResultSentences =
      (lowered.instruction as? Instruction.Transform)?.transformKind == CardOperation.TRANSFORM_KIND
  return RenderedAction(cost, result, condition, separateResultSentences, reducedCost?.second)
      .takeIf(RenderedAction::costCanJoinResult)
}

private data class RenderedAction(
    val cost: Predicate?,
    val result: RenderedInstructions,
    val condition: Clause? = null,
    val separateResultSentences: Boolean = false,
    val costExplanation: Clause.Simple? = null,
) {
  val unresolved: List<Unresolved>
    get() = result.unresolved

  fun withCostModifier(modifier: Modifier): RenderedAction? = cost?.let {
    copy(cost = it.withModifier(modifier))
  }

  fun asSentences(): Rendering<String> {
    if (condition == null) {
      if (cost == null) return result.asSentences()
      val infinitive = checkNotNull(result.asActionResultInfinitive())
      val first =
          if (!separateResultSentences) {
            Sentence(Clause.Simple(cost.withModifier(Modifier.Purpose(infinitive)))).render()
          } else {
            Sentence(Clause.Simple(cost.withModifier(Modifier.Purpose(result.clauses.first()))))
                .render()
          }
      val remaining =
          if (separateResultSentences) result.clauses.drop(1).map { Sentence(it).render() }
          else emptyList()
      val explanation = costExplanation?.let { listOf(Sentence(it).render()) }.orEmpty()
      return joinRenderings(listOf(first) + remaining + explanation)
    }
    val clause =
        cost?.let {
          Clause.Simple(it.withModifier(Modifier.Purpose(result.asCoordinatedClause())))
        } ?: result.asCoordinatedClause()
    return Sentence(Clause.Prefaced(Clause.Preface.Conditional(condition), clause)).render()
  }

  fun asAlternative(): Clause? {
    if (costExplanation != null) return null
    val clause =
        cost?.let {
          val infinitive = result.asActionResultInfinitive() ?: return null
          Clause.Simple(it.withModifier(Modifier.Purpose(infinitive)))
        } ?: result.clauses.singleOrNull() ?: return null
    return condition?.let {
      Clause.Prefaced(Clause.Preface.Conditional(it), clause)
    } ?: clause
  }

  fun costCanJoinResult(): Boolean =
      (cost == null || result.asActionResultInfinitive() != null) &&
          (costExplanation == null || condition == null)
}

private fun RenderedInstructions.asActionResultInfinitive(): Clause? = takeIf {
  clauses.all(Clause::canBeInfinitive)
}
    ?.asCoordinatedClause()

private fun Clause.canBeInfinitive(): Boolean =
    when (this) {
      is Clause.Simple -> subject == null
      is Clause.Coordinated -> clauses.members.all(Clause::canBeInfinitive)
      is Clause.RawPets -> true
      is Clause.Either,
      is Clause.Prefaced,
      is Clause.SharedSubject -> false
    }
