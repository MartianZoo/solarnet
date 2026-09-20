package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Effect.Trigger.XTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.tfm.text.ComponentDescriber.TriggerFrame as TriggerFrame

internal fun renderEffect(
    effect: Effect,
    describers: Describers,
): Rendering<String> {
  val lowered = describers.prepareForRendering(effect)
  if (isEndEffect(lowered, describers)) {
    return renderEndEffect(lowered, describers)
        ?: Rendering.unresolved(
            effect,
            RefusalReason.UNSUPPORTED_END_EFFECT,
            completeSentence("[$effect]"),
        )
  }
  val rendered =
      renderRemovalPrevention(lowered, describers)
          ?: renderPurchaseAdjustment(lowered, describers)
          ?: paymentDiscount(lowered, describers)?.let { renderPaymentDiscount(listOf(it)) }
          ?: renderResourcePaymentValue(lowered, describers)
          ?: renderCardResourcePaymentValue(lowered, describers)
          ?: renderAcceptedPaymentResource(lowered, describers)
          ?: renderRequirementFlexibility(lowered, describers)
          ?: renderLinkedPlayedTagResourceChoice(lowered, describers)
          ?: renderTriggeredInstructions(lowered, describers)
  return rendered
      ?: Rendering.unresolved(
          effect,
          RefusalReason.UNSUPPORTED_EFFECT_TRIGGER,
          completeSentence("[$effect]"),
      )
}

private fun renderCardResourcePaymentValue(
    effect: Effect,
    describers: Describers,
): Rendering<String>? {
  val choice = effect.instruction as? Instruction.Or ?: return null
  if (choice.instructions.size != 2) return null
  val sequence = choice.instructions.filterIsInstance<Then>().singleOrNull() ?: return null
  val decline = choice.instructions.singleOrNull { it !== sequence } ?: return null
  if (InstructionGroup.of(decline).instructions.singleOrNull() !is NoOp) return null
  val resourceRemoval = sequence.stages.singleOrNull() as? Remove ?: return null
  val resolvedResource = describers.resolveCardResource(resourceRemoval.removing) ?: return null
  if (
      resourceRemoval.quantifier.modality() != Modality.REQUIRED ||
          !describers.cardResourceHasHolder(resolvedResource, describers.thisExpression) ||
          resourceRemoval.removing.refinement != null ||
          !describers.isCardResource(resourceRemoval.removing.className)
  ) {
    return null
  }
  val resourceScalar = resourceRemoval.count.variableQuantity() ?: return null
  if (resourceScalar.multiple != 1) return null
  val owed = sequence.continuation as? Remove ?: return null
  if (owed.quantifier.modality() != Modality.BEST_EFFORT || owed.removing.refinement != null) {
    return null
  }
  if (
      describers.fact(owed.removing.className, ComponentDescriber::paymentRole) !=
          ComponentDescriber.PaymentRole.OWED
  ) {
    return null
  }
  val currency = describers.representedClass(owed.removing) ?: return null
  val rate = owed.count.variableQuantity()?.multiple ?: return null
  describers.plainGainNoun(currency.className, rate) ?: return null
  val currencyPhrase = describers.componentNounPhrase(currency.className, rate)
  val resources = describers.cardResourceNoun(resourceRemoval.removing.className, 2) ?: return null
  val trigger = describers.renderEventTrigger(effect.trigger) ?: return null
  val result =
      Clause.Simple(
          subject = NounPhrase.plural(resources).withModifier(Modifier.Phrase("on this card")),
          predicate =
              Predicate(
                  Verb("may be used"),
                  modifiers = listOf(resourceValueModifier(currencyPhrase)),
              ),
      )
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(trigger), result)).render()
}

private fun renderLinkedPlayedTagResourceChoice(
    effect: Effect,
    describers: Describers,
): Rendering<String>? {
  val trigger = (effect.trigger as? OnGainOf)?.expression ?: return null
  if (trigger.refinement != null) return null
  val holderKey = Key(ClassName.cn("Tag"), 0)
  val resolvedTrigger = describers.resolveExpression(trigger) ?: return null
  val holder = resolvedTrigger.sourceDependency(holderKey)?.takeIf { it.simple } ?: return null
  if (!resolvedTrigger.hasOnlySourceDependency(holderKey, holder)) return null
  val tagPhrase = describers.playedTagPhrase(trigger.className) ?: return null
  val alternatives = (effect.instruction as? Instruction.Or)?.instructions ?: return null
  var linkedDestination = false
  val clauses = alternatives.map { alternative ->
    renderLinkedCardResourceGain(alternative, holder, describers)?.also { linkedDestination = true }
        ?: renderInstructions(alternative, describers).clauses.singleOrNull()
        ?: return null
  }
  if (!linkedDestination) return null
  val event =
      eventTrigger(
          subject = NounPhrase.you(),
          verb = Verb("play"),
          objectPhrase = tagPhrase,
      )
  val result = Clause.Coordinated(Coordination(clauses, Conjunction.OR))
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(event), result)).render()
}

private fun renderLinkedCardResourceGain(
    instruction: InstructionTree,
    holder: Expression,
    describers: Describers,
): Clause.Simple? {
  val gain = InstructionGroup.of(instruction).instructions.singleOrNull() as? Gain ?: return null
  val resolved = describers.resolveCardResource(gain.gaining) ?: return null
  if (
      gain.quantifier.modality() != Modality.REQUIRED ||
          !describers.cardResourceHasHolder(resolved, holder) ||
          gain.gaining.refinement != null
  ) {
    return null
  }
  val count = gain.count.fixedQuantity() ?: return null
  val resource = describers.cardResourceNounPhrase(gain.gaining.className, count) ?: return null
  return Clause.Simple(
      Predicate(
          Verb("add"),
          Coordination.one(resource),
          listOf(Modifier.Phrase("to that card")),
      )
  )
}

private fun renderRequirementFlexibility(
    effect: Effect,
    describers: Describers,
): Rendering<String>? {
  val result = renderRequirementFlexibilityResult(effect, describers) ?: return null
  val event =
      eventTrigger(
          subject = NounPhrase.you(),
          verb = Verb("play"),
          objectPhrase = NounPhrase("card", determiner = Determiner.INDEFINITE),
      )
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(event), result)).render()
}

internal fun renderRequirementFlexibilityResult(
    effect: Effect,
    describers: Describers,
    card: NounPhrase? = null,
): Clause.Simple? {
  val trigger = effect.trigger as? OnGainOf ?: return null
  if (
      !trigger.expression.simple ||
          describers.triggerFrame(trigger.expression.className) !is TriggerFrame.PlayCard
  ) {
    return null
  }
  val removal = effect.instruction as? Remove ?: return null
  if (
      describers.resolvedRemovalModality(removal) != Modality.BEST_EFFORT ||
          removal.removing.refinement != null ||
          describers.fact(
              removal.removing.className,
              ComponentDescriber::requirementShortfall,
          ) != true
  ) {
    return null
  }
  val count = removal.count.fixedQuantity() ?: return null
  val target = describers.representedClass(removal.removing) ?: return null
  val requirementKind =
      describers.scaleFrame(target.className)?.subject
          ?: describers.fact(target.className, ComponentDescriber::requirementKind)
          ?: return null
  val steps = stepCount(count)
  val requirement =
      NounPhrase(
              "$requirementKind requirement",
              determiner = if (card == null) Determiner.INDEFINITE else Determiner.THE,
          )
          .let { noun -> card?.let { noun.withModifier(Modifier.Relation("of", it)) } ?: noun }
  return Clause.Simple(
      subject = NounPhrase.you(),
      predicate =
          Predicate(
              Verb("may treat"),
              Coordination.one(requirement),
              listOf(Modifier.Phrase("as if it is $steps lower or higher")),
          ),
  )
}

private fun renderPurchaseAdjustment(
    effect: Effect,
    describers: Describers,
): Rendering<String>? {
  val trigger = effect.trigger as? OnGainOf ?: return null
  when (describers.triggerFrame(trigger.expression.className)) {
    is TriggerFrame.PayingFor,
    is TriggerFrame.Purchase -> Unit
    else -> return null
  }
  if (describers.renderEvent(trigger)?.kind != Event.Kind.BUY) return null
  val triggerClause = describers.renderEventTrigger(trigger) ?: return null
  val change = effect.instruction as? Instruction.Change ?: return null
  val adjustment =
      when (change) {
        is Gain ->
            paymentResourceGain(
                change,
                ComponentDescriber.PaymentRole.OWED,
                describers,
            )
        is Remove -> owedReduction(change, describers)
        is Instruction.Transmute -> null
      } ?: return null
  val direction = if (change is Gain) "extra" else "less"
  return Sentence(
          Clause.Prefaced(
              Clause.Preface.Temporal(triggerClause),
              Clause.Simple(
                  predicate =
                      Predicate(
                          Verb("pay"),
                          Coordination.one(
                              adjustment.phrase.withModifier(Modifier.Phrase(direction))
                          ),
                      ),
              ),
          )
      )
      .render()
}

private fun renderAcceptedPaymentResource(
    effect: Effect,
    describers: Describers,
): Rendering<String>? {
  val gain = effect.instruction as? Gain ?: return null
  val acceptance =
      paymentResourceGain(
          gain,
          ComponentDescriber.PaymentRole.ACCEPTANCE,
          describers,
      ) ?: return null
  if (acceptance.count != 1) return null
  val resource = describers.representedClass(gain.gaining) ?: return null
  val noun = describers.plainGainCategoryNoun(resource.className, 2) ?: return null
  val trigger =
      describers.renderActionPaymentTrigger(effect.trigger)
          ?: describers.renderEventTrigger(effect.trigger)
          ?: return null
  val result =
      Clause.Simple(subject = NounPhrase.plural(noun), predicate = Predicate(Verb("may be used")))
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(trigger), result)).render()
}

internal fun acceptedFirstActionPaymentResource(
    effect: Effect,
    singleAction: Boolean,
    describers: Describers,
): String? {
  val gain = effect.instruction as? Gain ?: return null
  val acceptance =
      paymentResourceGain(
          gain,
          ComponentDescriber.PaymentRole.ACCEPTANCE,
          describers,
      ) ?: return null
  if (acceptance.count != 1) return null

  val actionUse = describers.actionUseEvent(effect.trigger) ?: return null
  if (actionUse.provider != describers.thisExpression) return null
  if (
      actionUse.slot != ClassName.cn("Action1").expression &&
          !(singleAction && actionUse.slot == null)
  ) {
    return null
  }
  return acceptance.noun
}

private fun Describers.renderActionPaymentTrigger(trigger: Trigger): Clause.Simple? {
  val action = actionUseEvent(trigger)?.provider ?: return null
  val objectPhrase =
      if (action == thisExpression) NounPhrase.text("this action")
      else renderActionUse(action) ?: return null
  return eventTrigger(
      subject = NounPhrase.you(),
      verb = Verb("pay for"),
      objectPhrase = objectPhrase,
  )
}

private fun renderRemovalPrevention(
    effect: Effect,
    describers: Describers,
): Rendering<String>? {
  if (!effect.automatic || !isDeadEndInstruction(effect.instruction, describers)) return null
  val (trigger, actor) =
      when (val authoredTrigger = effect.trigger) {
        is ByTrigger -> authoredTrigger.inner to authoredTrigger.by
        else -> authoredTrigger to null
      }
  val removed =
      when (trigger) {
        is OnRemoveOf -> listOf(trigger.expression)
        is Trigger.Or -> trigger.triggers.map { (it as? OnRemoveOf)?.expression ?: return null }
        else -> return null
      }
  val nouns = removed.map { protectedResourceNoun(it, describers) ?: return null }
  val resources = if (nouns.size == 1) nouns.single() else englishAlternatives(nouns)
  return when {
    actor == null &&
        removed.all {
          if (!describers.isCardResource(it.className)) return@all false
          val resolved = describers.resolveCardResource(it) ?: return@all false
          describers.cardResourceHasHolder(resolved, describers.thisExpression)
        } -> Rendering.resolved(completeSentence("$resources may not be removed from this card"))
    actor != null && describers.isNotOwner(actor) && removed.all(Expression::simple) ->
        Rendering.resolved(completeSentence("opponents may not remove your $resources"))
    else -> null
  }
}

private fun isDeadEndInstruction(
    instruction: InstructionTree,
    describers: Describers,
): Boolean {
  val gain = instruction as? Gain ?: return false
  if (gain.quantifier.modality() != Modality.REQUIRED) return false
  return gain.gaining.simple &&
      describers.concrete(gain.gaining.className) &&
      describers.fact(gain.gaining.className, ComponentDescriber::deadEndSignal) == true &&
      gain.count.fixedQuantity() == 1
}

private fun protectedResourceNoun(expression: Expression, describers: Describers): String? {
  if (!describers.concrete(expression.className) || expression.refinement != null) {
    return null
  }
  describers.cardResourceNoun(expression.className, 2)?.let {
    return it
  }
  if (!describers.isStandardResource(expression.className)) {
    return null
  }
  return describers.componentNoun(expression.className, 2)
}

internal fun renderEffects(
    effects: List<Effect>,
    describers: Describers,
    cardResourceType: ClassName? = null,
): Rendering<String> {
  val sentences = mutableListOf<Rendering<String>>()
  var index = 0
  while (index < effects.size) {
    renderOncePerActionProductionReward(effects.drop(index), describers)?.let { (sentence, consumed)
      ->
      sentences += sentence
      index += consumed
      continue
    }
    renderAcceptedResourcePayment(effects.drop(index), describers)?.let { (sentence, consumed) ->
      sentences += sentence
      index += consumed
      continue
    }
    renderAcceptedCardResourcePayment(effects.drop(index), cardResourceType, describers)?.let {
        (sentence, consumed) ->
      sentences += sentence
      index += consumed
      continue
    }
    val discount = paymentDiscount(effects[index], describers)
    if (discount == null) {
      val effect = effects[index]
      val rendering = renderEffect(effect, describers)
      sentences += rendering
      index++
      continue
    }
    val run =
        effects
            .drop(index)
            .map { paymentDiscount(it, describers) }
            .takeWhile {
              it?.reduction == discount.reduction &&
                  it?.categoryReduction == discount.categoryReduction
            }
    sentences += renderPaymentDiscount(run.filterNotNull())
    index += run.size
  }
  return joinRenderings(sentences)
}

private fun renderOncePerActionProductionReward(
    effects: List<Effect>,
    describers: Describers,
): Pair<Rendering<String>, Int>? {
  val actionEnable = effects.getOrNull(0)?.let(describers::prepareForRendering) ?: return null
  val phaseEnable = effects.getOrNull(1)?.let(describers::prepareForRendering) ?: return null

  val rewardMarker = enabledLatchMarker(actionEnable) ?: return null
  if (!resetsAfterAction(actionEnable.trigger, describers)) return null
  if (enabledLatchMarker(phaseEnable) != rewardMarker) return null
  if (!resetsForPreludeAction(phaseEnable.trigger)) return null

  val declaration = describers.declaration(rewardMarker.className)
  if (!hasUnitLatchInvariant(declaration.invariants, describers.thisExpression)) return null
  val rewardEffect = declaration.effects.singleOrNull() ?: return null
  if (!rewardEffect.automatic) return null
  val sizedTrigger = rewardEffect.trigger as? XTrigger ?: return null
  val productionTrigger = sizedTrigger.inner as? OnGainOf ?: return null
  val production =
      productionCategoryExpression(productionTrigger.expression, describers) ?: return null
  if (production.owner != null || describers.concrete(production.resource)) return null

  val rewardInstructions = InstructionGroup.of(rewardEffect.instruction).instructions
  val markerRemoval = rewardInstructions.lastOrNull() as? Remove ?: return null
  if (
      markerRemoval.removing != describers.thisExpression ||
          markerRemoval.quantifier.modality() != Modality.REQUIRED ||
          markerRemoval.count.fixedQuantity() != 1
  ) {
    return null
  }
  val reward = renderInstructions(InstructionGroup(rewardInstructions.dropLast(1)), describers)
  val rewardClause = reward.clauses.singleOrNull() as? Clause.Simple ?: return null
  if (reward.unresolved.isNotEmpty()) return null
  val conditionalReward =
      rewardClause.withModifier(Modifier.Phrase("if you increase any production"))
  return Sentence(
          Clause.Prefaced(
              Clause.Preface.OncePerAction,
              conditionalReward,
          )
      )
      .render() to 2
}

private fun enabledLatchMarker(effect: Effect): Expression? {
  val gain = effect.instruction as? Gain ?: return null
  if (
      !effect.automatic ||
          gain.quantifier.modality() != Modality.BEST_EFFORT ||
          gain.count.fixedQuantity() != 1 ||
          gain.gaining.refinement != null
  ) {
    return null
  }
  return gain.gaining
}

private fun resetsAfterAction(trigger: Trigger, describers: Describers): Boolean {
  val action = describers.actionUseEvent(trigger) ?: return false
  if (action.slot != null) return false
  return describers.fact(action.provider.className, ComponentDescriber::actionUse)?.objectPhrase ==
      "an action"
}

private fun resetsForPreludeAction(trigger: Trigger): Boolean {
  val conditioned = trigger as? Trigger.IfTrigger ?: return false
  val turn = (conditioned.inner as? OnGainOf)?.expression ?: return false
  if (!turn.simple || turn.className != cn("NewTurn")) return false
  return countedPresence(conditioned.condition)?.className == cn("PreludePhase")
}

private fun countedPresence(requirement: Requirement): Expression? {
  val minimum = requirement as? Requirement.Min ?: return null
  if (minimum.minimum != 1) return null
  return (minimum.countedMetric as? Metric.Count)?.expression?.takeIf(Expression::simple)
}

private fun hasUnitLatchInvariant(
    invariants: Set<Requirement>,
    contextualThis: Expression,
): Boolean =
    invariants.singleOrNull()?.let { invariant ->
      val maximum = invariant as? Requirement.Max ?: return@let false
      val marker = (maximum.countedMetric as? Metric.Count)?.expression ?: return@let false
      maximum.maximum == 1 && marker == contextualThis
    } == true

private fun renderAcceptedResourcePayment(
    effects: List<Effect>,
    describers: Describers,
): Pair<Rendering<String>, Int>? {
  val acceptance = effects.getOrNull(0) ?: return null
  val payment = effects.getOrNull(1) ?: return null
  val accepted =
      paymentResourceGain(
          acceptance.instruction,
          ComponentDescriber.PaymentRole.ACCEPTANCE,
          describers,
      ) ?: return null
  if (accepted.count != 1 || accepted.resource == null) return null
  if (describers.hasBasePaymentValue(accepted.resource)) return null
  val spent =
      describers.resourcePaymentEvent(payment.trigger) as? ResourcePaymentEvent.Standard
          ?: return null
  if (spent.resource.className != accepted.resource) return null
  val reduction = owedReduction(payment.instruction, describers) ?: return null
  val rendered =
      renderAcceptedResourceValue(
          acceptance.trigger,
          accepted,
          reduction.phrase,
          describers,
      ) ?: return null
  return rendered to 2
}

internal fun renderAcceptedResourceValue(
    trigger: Trigger,
    accepted: ResourceAmount,
    valuePhrase: NounPhrase,
    describers: Describers,
): Rendering<String>? {
  val resourceClassName = accepted.resource ?: return null
  if (accepted.count != 1) return null
  if (describers.hasBasePaymentValue(resourceClassName)) return null
  val resource = describers.componentNoun(resourceClassName, 2)
  val triggerClause = describers.renderEventTrigger(trigger) ?: return null
  val result =
      Clause.Simple(
          subject = NounPhrase.plural(resource),
          predicate =
              Predicate(
                  Verb("may be used"),
                  modifiers = listOf(resourceValueModifier(valuePhrase)),
              ),
      )
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(triggerClause), result)).render()
}

private fun renderAcceptedCardResourcePayment(
    effects: List<Effect>,
    cardResourceType: ClassName?,
    describers: Describers,
): Pair<Rendering<String>, Int>? {
  cardResourceType ?: return null
  val acceptance = effects.getOrNull(0) ?: return null
  val payment = effects.getOrNull(1) ?: return null
  val accepted =
      InstructionGroup.of(acceptance.instruction).instructions.singleOrNull() as? Gain
          ?: return null
  if (
      describers.fact(accepted.gaining.className, ComponentDescriber::paymentRole) !=
          ComponentDescriber.PaymentRole.ACCEPTANCE
  ) {
    return null
  }
  val acceptingKey = Key(ClassName.cn("AcceptingFromCard"), 0)
  val resolvedAccepted = describers.resolveExpression(accepted.gaining, acceptingKey) ?: return null
  if (
      accepted.quantifier.modality() != Modality.REQUIRED ||
          !resolvedAccepted.hasOnlySourceDependency(acceptingKey, describers.thisExpression) ||
          accepted.gaining.refinement != null ||
          accepted.count.fixedQuantity() != 1
  ) {
    return null
  }
  val spent =
      describers.resourcePaymentEvent(payment.trigger) as? ResourcePaymentEvent.FromCard
          ?: return null
  if (spent.card != describers.thisExpression) return null
  val reduction = owedReduction(payment.instruction, describers) ?: return null
  val resource = describers.cardResourceNoun(cardResourceType, 2) ?: return null
  val billing = describers.billingEvent(acceptance.trigger)
  if (billing?.provider?.className == HAS_ACTIONS) {
    val result =
        Clause.Simple(
            subject = NounPhrase.you(),
            predicate =
                Predicate(
                    Verb("may use"),
                    Coordination.one(
                        NounPhrase.text(resource).withModifier(Modifier.Phrase("on this card"))
                    ),
                    listOf(resourceValueModifier(reduction.phrase)),
                ),
        )
    return Sentence(result).render() to 2
  }
  val trigger = describers.renderEventTrigger(acceptance.trigger) ?: return null
  val result =
      Clause.Simple(
          subject = NounPhrase.plural(resource).withModifier(Modifier.Phrase("on this card")),
          predicate =
              Predicate(
                  Verb("may be used"),
                  modifiers = listOf(resourceValueModifier(reduction.phrase)),
              ),
      )
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(trigger), result)).render() to 2
}

private fun resourceValueModifier(value: NounPhrase): Modifier.Relation =
    Modifier.Relation(
        "as",
        value.withModifier(Modifier.Phrase("each")),
    )

internal fun paymentDiscount(effect: Effect, describers: Describers): PaymentDiscount? {
  (effect.trigger as? Trigger.Or)?.let { alternatives ->
    val discounts =
        alternatives.triggers.map { trigger ->
          paymentDiscount(effect.copy(trigger = trigger), describers) ?: return null
        }
    val first = discounts.firstOrNull() ?: return null
    if (
        discounts.any {
          it.reduction != first.reduction || it.categoryReduction != first.categoryReduction
        }
    ) {
      return null
    }
    return first.copy(trigger = coordinatePaymentTriggers(discounts.map(PaymentDiscount::trigger)))
  }
  completeOwedReduction(effect.instruction, describers)?.let { reduction ->
    val trigger = describers.renderPaymentDiscountTrigger(effect.trigger) ?: return null
    if (!trigger.accepts(reduction)) return null
    return PaymentDiscount(
        trigger.clause,
        reduction.copy(count = 0),
        objectPronoun = trigger.objectPronoun,
    )
  }
  owedReduction(effect.instruction, describers)?.let { reduction ->
    val actionTrigger = describers.renderPaymentDiscountTrigger(effect.trigger)
    if (actionTrigger?.accepts(reduction) == false) return null
    val trigger =
        actionTrigger?.clause
            ?: (describers.renderEventTrigger(effect.trigger) as? Clause.Simple ?: return null)
    return PaymentDiscount(
        trigger,
        reduction,
        objectPronoun = actionTrigger?.objectPronoun ?: trigger.hasExplicitObject(),
    )
  }
  val trigger = describers.renderPaymentDiscountTrigger(effect.trigger) ?: return null
  val actualReduction = describers.renderPlainGainAmount(effect.instruction) ?: return null
  val reduction =
      trigger.categoryNoun?.let { noun ->
        actualReduction.withNoun(noun)
      } ?: actualReduction
  return PaymentDiscount(
      trigger.clause,
      reduction,
      categoryReduction = trigger.categoryNoun != null,
      objectPronoun = trigger.objectPronoun,
  )
}

private fun renderPaymentDiscount(discounts: List<PaymentDiscount>): Rendering<String> {
  val trigger = coordinatePaymentTriggers(discounts.map(PaymentDiscount::trigger).distinct())
  val reduction = discounts.first().reduction
  val result =
      if (reduction.count == 0) {
        Clause.Simple(
            subject = NounPhrase.text("the cost"),
            predicate = Predicate(Verb.BE, Coordination.one(reduction.phrase)),
        )
      } else if (discounts.first().categoryReduction) {
        Clause.Simple(
            subject = NounPhrase.you(),
            predicate =
                Predicate(
                    Verb("pay"),
                    Coordination.one(NounPhrase.text("${reduction.count} less ${reduction.noun}")),
                    modifiers =
                        listOfNotNull(
                            Modifier.Phrase("for it").takeIf { discounts.first().objectPronoun }
                        ),
                ),
        )
      } else {
        Clause.Simple(
            subject = NounPhrase.you(),
            predicate =
                Predicate(
                    Verb("pay"),
                    Coordination.one(reduction.phrase.withModifier(Modifier.Phrase("less"))),
                    modifiers =
                        listOfNotNull(
                            Modifier.Phrase("for it").takeIf { discounts.first().objectPronoun }
                        ),
                ),
        )
      }
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(trigger), result)).render()
}

private fun coordinatePaymentTriggers(clauses: List<Clause>): Clause {
  if (clauses.size == 1) return clauses.single()
  val simple = clauses.filterIsInstance<Clause.Simple>().takeIf { it.size == clauses.size }
  val actingPlayer = NounPhrase.you()
  return if (simple != null && simple.all { it.subject == actingPlayer }) {
    Clause.SharedSubject(
        actingPlayer,
        Coordination(simple.map(Clause.Simple::predicate), Conjunction.OR),
    )
  } else {
    Clause.Coordinated(Coordination(clauses, Conjunction.OR))
  }
}

private fun Clause.hasExplicitObject(): Boolean =
    when (this) {
      is Clause.Simple -> predicate.objects != null
      is Clause.SharedSubject -> predicates.members.all { it.objects != null }
      is Clause.Coordinated -> clauses.members.all(Clause::hasExplicitObject)
      is Clause.Either -> alternatives.members.all(Clause::hasExplicitObject)
      is Clause.Prefaced,
      is Clause.RawPets -> false
    }

private fun renderResourcePaymentValue(
    effect: Effect,
    describers: Describers,
): Rendering<String>? {
  val spent = describers.renderSpentResource(effect.trigger) ?: return null
  val reduction = owedReduction(effect.instruction, describers) ?: return null
  return Rendering.resolved("Each $spent you pay is worth ${reduction.phrase.linearize()} extra.")
}

private fun Describers.renderEventTrigger(trigger: Trigger): Clause? {
  renderAbstractTagTrigger(trigger)?.let {
    return it
  }
  val events =
      when (trigger) {
        is Trigger.Or -> trigger.triggers.map { renderTriggerClause(it) ?: return null }
        else -> listOf(renderTriggerClause(trigger) ?: return null)
      }
  if (events.size == 1) return events.single()
  return coordinateClauseObjects(events, Conjunction.OR)
      ?: coordinateSharedSubjectPredicates(events)
      ?: Clause.Coordinated(Coordination(events, Conjunction.OR))
}

private fun coordinateSharedSubjectPredicates(clauses: List<Clause.Simple>): Clause.SharedSubject? {
  val subject = clauses.firstOrNull()?.subject ?: return null
  if (clauses.any { it.subject != subject }) return null
  return Clause.SharedSubject(
      subject,
      Coordination(clauses.map(Clause.Simple::predicate), Conjunction.COMMA_OR),
  )
}

private fun Describers.renderAbstractTagTrigger(trigger: Trigger): Clause.Simple? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null) return null
  if (triggerFrame(expression.className) !is TriggerFrame.PlayTag) {
    return null
  }
  val represented = representedClass(expression) ?: return null
  playedTagPhrase(represented.className)?.let { phrase ->
    return eventTrigger(
        subject = NounPhrase.you(),
        verb = Verb("play"),
        objectPhrase = phrase,
    )
  }
  val tags = expressions.concreteSubclassesOf(represented.className)
  if (tags.size < 2) return null
  val objects = tags.map { tag ->
    playedTagPhrase(tag) ?: return null
  }
  return Clause.Simple(
      subject = NounPhrase.you(),
      predicate = Predicate(Verb("play"), Coordination(objects, Conjunction.OR)),
  )
}

private val HAS_ACTIONS = ClassName.cn("HasActions")

private fun Describers.renderTriggerClause(trigger: Trigger): Clause.Simple? =
    billingEvent(trigger)?.let { renderBillingEvent(it) }
        ?: renderOperationTrigger(trigger)
        ?: renderEvent(trigger)?.renderTrigger()

private fun Describers.renderBillingEvent(billing: BillingEvent): Clause.Simple? {
  billing.card?.let { card ->
    return playedCardEvent(card)?.renderTrigger()
  }
  if (billing.phase == BillingEvent.Phase.COMPLETED) {
    return eventTrigger(
        subject = NounPhrase.you(),
        verb = Verb("pay for"),
        objectPhrase = renderActionUse(billing.provider) ?: return null,
    )
  }
  val predicate =
      fact(billing.provider.className, ComponentDescriber::actionUse)?.paymentDiscount?.predicate
          ?: return null
  return eventTrigger(subject = NounPhrase.you(), verb = Verb(predicate))
}

private fun Describers.renderOperationTrigger(trigger: Trigger): Clause.Simple? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null) return null
  val operation =
      (changeFrame(expression.className) as? ComponentDescriber.ChangeFrame.Procedure)?.takeIf {
        it.objectPhrase == null
      } ?: return null
  return eventTrigger(subject = NounPhrase.you(), verb = Verb(operation.verb))
}

private fun Describers.renderSpentResource(trigger: Trigger): String? {
  val payment = resourcePaymentEvent(trigger) as? ResourcePaymentEvent.Standard ?: return null
  return plainGainCategoryNoun(payment.resource.className, 1)
}

private fun Describers.renderPaymentDiscountTrigger(
    trigger: Trigger,
): PaymentDiscount.Trigger? =
    renderActionPaymentDiscountTrigger(trigger) ?: renderBillingPaymentDiscountTrigger(trigger)

private fun Describers.renderBillingPaymentDiscountTrigger(
    trigger: Trigger,
): PaymentDiscount.Trigger? {
  val billing = billingEvent(trigger) ?: return null
  if (billing.phase != BillingEvent.Phase.STARTED) return null
  val use = fact(billing.provider.className, ComponentDescriber::actionUse) ?: return null
  val discount = use.paymentDiscount ?: return null
  return PaymentDiscount.Trigger(
      renderBillingEvent(billing) ?: return null,
      discount.categoryNoun,
      billing.resource?.className,
      discount.objectPronoun,
  )
}

private fun Describers.renderActionPaymentDiscountTrigger(
    trigger: Trigger,
): PaymentDiscount.Trigger? {
  val action = actionUseEvent(trigger)?.provider?.takeIf { it.simple } ?: return null
  if (action == thisExpression) return null
  val use = fact(action.className, ComponentDescriber::actionUse) ?: return null
  val discount = use.paymentDiscount ?: return null
  return PaymentDiscount.Trigger(
      eventTrigger(subject = NounPhrase.you(), verb = Verb(discount.predicate)),
      discount.categoryNoun,
      objectPronoun = discount.objectPronoun,
  )
}

private fun Describers.renderPlainGainAmount(instruction: InstructionTree): ResourceAmount? {
  val change = instruction as? Instruction ?: return null
  val (className, count) = standardResourceGain(change, this) ?: return null
  return ResourceAmount(
      count,
      componentNoun(className, 1),
      componentNoun(className, 2),
      className,
  )
}

private fun renderTriggeredInstructions(
    effect: Effect,
    describers: Describers,
): Rendering<String>? {
  val trigger = describers.renderEventTrigger(effect.trigger) ?: return null
  val instruction =
      if (
          effect.trigger.descendantsOfType<Expression>().contains(describers.anyoneExpression) &&
              effect.instruction
                  .descendantsOfType<Expression>()
                  .contains(describers.anyoneExpression)
      ) {
        PetNode.replacer(describers.anyoneExpression, describers.playerExpression)
            .transformInstructionTree(effect.instruction)
      } else {
        effect.instruction
      }
  val result =
      renderPreparedInstructions(instruction, describers, TypeVariableReferences.from(effect))
  val compound =
      InstructionGroup.of(instruction).instructions.any {
        it is Instruction.Or || it is Instruction.Then
      }
  if (compound && result.unresolved.isNotEmpty()) return null
  return Sentence(
          Clause.Prefaced(
              Clause.Preface.Temporal(trigger),
              result.asCoordinatedClause(),
          )
      )
      .render()
}
