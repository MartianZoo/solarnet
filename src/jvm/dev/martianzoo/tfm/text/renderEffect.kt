package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.tfm.text.ComponentDescriber.TriggerFrame as TriggerFrame

internal fun renderEffect(
    effect: Effect,
    describers: Describers,
): Rendering<String> {
  val lowered = lowerProductionSyntax(effect)
  val rendered =
      if (isEndEffect(lowered, describers)) {
        renderEndEffect(lowered, describers)
      } else {
        renderRemovalPrevention(lowered, describers)
            ?: renderPurchaseAdjustment(lowered, describers)
            ?: paymentDiscount(lowered, describers)?.let { renderPaymentDiscount(listOf(it)) }
            ?: renderResourcePaymentValue(lowered, describers)
            ?: renderCardResourcePaymentValue(lowered, describers)
            ?: renderAcceptedPaymentResource(lowered, describers)
            ?: renderRequirementFlexibility(lowered, describers)
            ?: renderLinkedPlayedTagResourceChoice(lowered, describers)
            ?: renderLinkedProductionReward(lowered, describers)
            ?: renderTriggeredInstructions(lowered, describers)
      }
  return rendered?.let(Rendering.Companion::resolved)
      ?: Rendering.unresolved(
          effect,
          if (isEndEffect(lowered, describers)) {
            RefusalReason.UNSUPPORTED_END_EFFECT
          } else {
            RefusalReason.UNSUPPORTED_EFFECT_TRIGGER
          },
          completeSentence("[$effect]"),
      )
}

private fun renderCardResourcePaymentValue(effect: Effect, describers: Describers): String? {
  val choice = effect.instruction as? Instruction.Or ?: return null
  if (choice.instructions.size != 2) return null
  val sequence = choice.instructions.filterIsInstance<Then>().singleOrNull() ?: return null
  val decline = choice.instructions.singleOrNull { it !== sequence } ?: return null
  if (InstructionGroup.of(decline).instructions.singleOrNull() !is NoOp) return null
  val resourceRemoval = sequence.stages.singleOrNull() as? Remove ?: return null
  val resolvedResource = describers.resolveCardResource(resourceRemoval.removing) ?: return null
  if (
      resourceRemoval.intensity.modality() != Modality.REQUIRED ||
          !describers.cardResourceHasHolder(resolvedResource, describers.thisExpression) ||
          resourceRemoval.removing.refinement != null ||
          resourceRemoval.removing.complement ||
          !describers.isCardResource(resourceRemoval.removing.className)
  ) {
    return null
  }
  val resourceScalar = resourceRemoval.count.variableQuantity() ?: return null
  if (resourceScalar.multiple != 1) return null
  val owed = sequence.continuation as? Remove ?: return null
  if (
      owed.intensity.modality() != Modality.BEST_EFFORT ||
          owed.removing.refinement != null ||
          owed.removing.complement
  ) {
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
          subject = NounPhrase.text(resources).withModifier(Modifier.Phrase("on this card")),
          predicate =
              Predicate(
                  "may be used",
                  modifiers = listOf(resourceValueModifier(currencyPhrase)),
              ),
      )
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(trigger), result)).linearize()
}

private fun renderLinkedPlayedTagResourceChoice(
    effect: Effect,
    describers: Describers,
): String? {
  val trigger = (effect.trigger as? OnGainOf)?.expression ?: return null
  if (trigger.refinement != null || trigger.complement) return null
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
  val result = Clause.Coordinated(Coordination(clauses, Conjunction.OR))
  return completeSentence("when you play $tagPhrase, ${result.linearize()}")
}

private fun renderLinkedCardResourceGain(
    instruction: InstructionTree,
    holder: Expression,
    describers: Describers,
): Clause.Simple? {
  val gain = InstructionGroup.of(instruction).instructions.singleOrNull() as? Gain ?: return null
  val resolved = describers.resolveCardResource(gain.gaining) ?: return null
  if (
      gain.intensity.modality() != Modality.REQUIRED ||
          !describers.cardResourceHasHolder(resolved, holder) ||
          gain.gaining.refinement != null ||
          gain.gaining.complement
  ) {
    return null
  }
  val count = gain.count.fixedQuantity() ?: return null
  val resource = describers.cardResourceNounPhrase(gain.gaining.className, count) ?: return null
  return Clause.Simple(
      Predicate(
          "add",
          Coordination.one(resource),
          listOf(Modifier.Phrase("to that card")),
      )
  )
}

private fun renderRequirementFlexibility(effect: Effect, describers: Describers): String? {
  val trigger = effect.trigger as? OnGainOf ?: return null
  if (
      !trigger.expression.simple ||
          describers.triggerFrame(trigger.expression.className) !is TriggerFrame.PlayCard
  ) {
    return null
  }
  val removal = effect.instruction as? Remove ?: return null
  if (
      removal.intensity.modality() != Modality.BEST_EFFORT ||
          removal.removing.refinement != null ||
          removal.removing.complement ||
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
  val steps = if (count == 1) "step" else "steps"
  return completeSentence(
      "when you play a card, you may treat a $requirementKind requirement as if it is " +
          "$count $steps lower or higher"
  )
}

private fun renderPurchaseAdjustment(effect: Effect, describers: Describers): String? {
  val trigger = effect.trigger as? OnGainOf ?: return null
  if (!trigger.expression.simple) return null
  if (describers.triggerFrame(trigger.expression.className) !is TriggerFrame.Purchase) return null
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
                          "pay",
                          Coordination.one(
                              adjustment.phrase.withModifier(Modifier.Phrase(direction))
                          ),
                      ),
              ),
          )
      )
      .linearize()
}

private fun renderAcceptedPaymentResource(effect: Effect, describers: Describers): String? {
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
  val result = Clause.Simple(subject = NounPhrase.text(noun), predicate = Predicate("may be used"))
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(trigger), result)).linearize()
}

internal fun acceptedFirstActionPaymentResource(
    effect: Effect,
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
  if (actionUse.slot != ClassName.cn("Action1").expression) return null
  return acceptance.noun
}

private fun Describers.renderActionPaymentTrigger(trigger: Trigger): Clause.Simple? {
  val action = actionUseEvent(trigger)?.provider ?: return null
  val objectPhrase =
      if (action == thisExpression) "this action" else renderActionUse(action) ?: return null
  return eventTrigger(
      subject = NounPhrase.text("you"),
      verb = "pay for",
      objectPhrase = NounPhrase.text(objectPhrase),
  )
}

private fun renderRemovalPrevention(effect: Effect, describers: Describers): String? {
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
        } -> completeSentence("$resources may not be removed from this card")
    actor == describers.notOwnerExpression && removed.all(Expression::simple) ->
        completeSentence("opponents may not remove your $resources")
    else -> null
  }
}

private fun isDeadEndInstruction(
    instruction: InstructionTree,
    describers: Describers,
): Boolean {
  val gain = instruction as? Gain ?: return false
  if (gain.intensity.modality() != Modality.REQUIRED) return false
  return gain.gaining.simple &&
      describers.concrete(gain.gaining.className) &&
      describers.fact(gain.gaining.className, ComponentDescriber::deadEndSignal) == true &&
      gain.count.fixedQuantity() == 1
}

private fun protectedResourceNoun(expression: Expression, describers: Describers): String? {
  if (
      !describers.concrete(expression.className) ||
          expression.refinement != null ||
          expression.complement
  ) {
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
  val sentences = mutableListOf<String>()
  val unresolved = mutableListOf<Unresolved>()
  var index = 0
  while (index < effects.size) {
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
    renderBarrierSequencedTrackChoice(effects.drop(index), describers)?.let { (sentence, consumed)
      ->
      sentences += sentence
      index += consumed
      continue
    }
    val discount = paymentDiscount(effects[index], describers)
    if (discount == null) {
      val effect = effects[index]
      val rendering = renderEffect(effect, describers)
      sentences += rendering.value
      unresolved += rendering.unresolved
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
  return Rendering(sentences.joinToString(" "), unresolved)
}

private fun renderAcceptedResourcePayment(
    effects: List<Effect>,
    describers: Describers,
): Pair<String, Int>? {
  val acceptance = effects.getOrNull(0) ?: return null
  val payment = effects.getOrNull(1) ?: return null
  val accepted =
      paymentResourceGain(
          acceptance.instruction,
          ComponentDescriber.PaymentRole.ACCEPTANCE,
          describers,
      ) ?: return null
  if (accepted.count != 1 || accepted.resource == null) return null
  if (accepted.resource == STEEL || accepted.resource == TITANIUM) return null
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
): String? {
  val resourceClassName = accepted.resource ?: return null
  if (accepted.count != 1) return null
  if (resourceClassName == STEEL || resourceClassName == TITANIUM) return null
  val resource = describers.componentNoun(resourceClassName, 2)
  val triggerClause = describers.renderEventTrigger(trigger) ?: return null
  val result =
      Clause.Simple(
          subject = NounPhrase.text(resource),
          predicate =
              Predicate(
                  "may be used",
                  modifiers = listOf(resourceValueModifier(valuePhrase)),
              ),
      )
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(triggerClause), result)).linearize()
}

private fun renderAcceptedCardResourcePayment(
    effects: List<Effect>,
    cardResourceType: ClassName?,
    describers: Describers,
): Pair<String, Int>? {
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
      accepted.intensity.modality() != Modality.REQUIRED ||
          !resolvedAccepted.hasOnlySourceDependency(acceptingKey, describers.thisExpression) ||
          accepted.gaining.refinement != null ||
          accepted.gaining.complement ||
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
            subject = NounPhrase.text("you"),
            predicate =
                Predicate(
                    "may use",
                    Coordination.one(
                        NounPhrase.text(resource).withModifier(Modifier.Phrase("on this card"))
                    ),
                    listOf(resourceValueModifier(reduction.phrase)),
                ),
        )
    return Sentence(result).linearize() to 2
  }
  val trigger = describers.renderEventTrigger(acceptance.trigger) ?: return null
  val result =
      Clause.Simple(
          subject = NounPhrase.text(resource).withModifier(Modifier.Phrase("on this card")),
          predicate =
              Predicate(
                  "may be used",
                  modifiers = listOf(resourceValueModifier(reduction.phrase)),
              ),
      )
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(trigger), result)).linearize() to 2
}

private fun resourceValueModifier(value: NounPhrase): Modifier.Relation =
    Modifier.Relation(
        "as",
        value.withModifier(Modifier.Phrase("each")),
    )

private fun renderBarrierSequencedTrackChoice(
    effects: List<Effect>,
    describers: Describers,
): Pair<String, Int>? {
  val barrierEffect = effects.getOrNull(0) ?: return null
  val trackEffect = effects.getOrNull(1) ?: return null
  if (
      !barrierEffect.automatic ||
          trackEffect.automatic ||
          barrierEffect.trigger != trackEffect.trigger
  ) {
    return null
  }
  val barrierGain =
      InstructionGroup.of(barrierEffect.instruction).instructions.singleOrNull() as? Gain
          ?: return null
  if (
      barrierGain.intensity.modality() != Modality.REQUIRED ||
          barrierGain.gaining.refinement != null ||
          barrierGain.gaining.complement ||
          barrierGain.count.fixedQuantity() != 1 ||
          describers.fact(barrierGain.gaining.className, ComponentDescriber::paymentRole) !=
              ComponentDescriber.PaymentRole.BARRIER
  ) {
    return null
  }
  val sequence = trackEffect.instruction as? Then ?: return null
  val trackGain = sequence.stages.singleOrNull() as? Gain ?: return null
  if (
      trackGain.intensity.modality() != Modality.OPTIONAL ||
          trackGain.gaining.refinement != null ||
          trackGain.gaining.complement ||
          trackGain.count.fixedQuantity() != 1
  ) {
    return null
  }
  val track = describers.scaleFrame(trackGain.gaining.className) ?: return null
  val barrierRemoval = sequence.continuation as? Remove ?: return null
  if (
      barrierRemoval.intensity.modality() != Modality.REQUIRED ||
          barrierRemoval.removing != barrierGain.gaining ||
          barrierRemoval.count.fixedQuantity() != 1
  ) {
    return null
  }
  val triggerExpression = (trackEffect.trigger as? OnGainOf)?.expression ?: return null
  val resolvedGain = describers.resolveExpression(trackGain.gaining) ?: return null
  val resolvedTrigger = describers.resolveExpression(triggerExpression) ?: return null
  val selectedTrack = resolvedGain.sourceDependencies.values.singleOrNull() ?: return null
  if (resolvedTrigger.sourceDependencies.values.singleOrNull() != selectedTrack) return null
  val barrierDependencies =
      describers.resolveExpression(barrierGain.gaining)?.sourceDependencies?.values ?: return null
  if (barrierDependencies.isNotEmpty() && barrierDependencies.singleOrNull() != selectedTrack) {
    return null
  }
  val trigger = describers.renderEventTrigger(trackEffect.trigger) ?: return null
  return completeSentence(
      "when ${trigger.linearize()}, you may first increase that ${track.subject} 1 step"
  ) to 2
}

private fun paymentDiscount(effect: Effect, describers: Describers): PaymentDiscount? {
  completeOwedReduction(effect.instruction, describers)?.let { reduction ->
    val trigger = describers.renderPaymentDiscountTrigger(effect.trigger) ?: return null
    if (!trigger.accepts(reduction)) return null
    return PaymentDiscount(trigger.clause, reduction.copy(count = 0))
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
  )
}

private fun renderPaymentDiscount(discounts: List<PaymentDiscount>): String {
  val clauses = discounts.map { it.trigger }.distinct()
  val actingPlayer = NounPhrase.text("you")
  val trigger =
      if (clauses.size == 1) {
        clauses.single()
      } else if (clauses.all { it.subject == actingPlayer }) {
        Clause.SharedSubject(
            actingPlayer,
            Coordination(clauses.map(Clause.Simple::predicate), Conjunction.OR),
        )
      } else {
        Clause.Coordinated(Coordination(clauses, Conjunction.OR))
      }
  val reduction = discounts.first().reduction
  val result =
      if (reduction.count == 0) {
        Clause.Simple(
            subject = NounPhrase.text("the cost"),
            predicate = Predicate("is", Coordination.one(reduction.phrase)),
        )
      } else if (discounts.first().categoryReduction) {
        Clause.Simple(
            Predicate(
                "pay",
                Coordination.one(NounPhrase.text("${reduction.count} less ${reduction.noun}")),
            )
        )
      } else {
        Clause.Simple(
            Predicate(
                "pay",
                Coordination.one(reduction.phrase.withModifier(Modifier.Phrase("less"))),
            )
        )
      }
  return Sentence(Clause.Prefaced(Clause.Preface.Temporal(trigger), result)).linearize()
}

private fun renderResourcePaymentValue(effect: Effect, describers: Describers): String? {
  val spent = describers.renderSpentResource(effect.trigger) ?: return null
  val reduction = owedReduction(effect.instruction, describers) ?: return null
  return "Each $spent you pay is worth ${reduction.phrase.linearize()} extra."
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
  if (expression.refinement != null || expression.complement) return null
  if (triggerFrame(expression.className) !is TriggerFrame.PlayTag) {
    return null
  }
  val represented = representedClass(expression) ?: return null
  playedTagPhrase(represented.className)?.let { phrase ->
    return eventTrigger(
        subject = NounPhrase.text("you"),
        verb = "play",
        objectPhrase = NounPhrase.text(phrase),
    )
  }
  val tags = expressions.concreteSubclassesOf(represented.className)
  if (tags.size < 2) return null
  val objects = tags.map { tag ->
    val name = tagName(tag)?.first ?: return null
    NounPhrase.text("${indefiniteArticle(name)} $name tag")
  }
  return Clause.Simple(
      subject = NounPhrase.text("you"),
      predicate = Predicate("play", Coordination(objects, Conjunction.OR)),
  )
}

private val HAS_ACTIONS = ClassName.cn("HasActions")
private val STEEL = ClassName.cn("Steel")
private val TITANIUM = ClassName.cn("Titanium")

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
        subject = NounPhrase.text("you"),
        verb = "pay for",
        objectPhrase = NounPhrase.text(renderActionUse(billing.provider) ?: return null),
    )
  }
  val predicate =
      fact(billing.provider.className, ComponentDescriber::actionUse)?.paymentDiscount?.predicate
          ?: return null
  return eventTrigger(subject = NounPhrase.text("you"), verb = predicate)
}

private fun Describers.renderOperationTrigger(trigger: Trigger): Clause.Simple? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  val operation =
      (changeFrame(expression.className) as? ComponentDescriber.ChangeFrame.Procedure)?.takeIf {
        it.objectPhrase == null
      } ?: return null
  return eventTrigger(subject = NounPhrase.text("you"), verb = operation.verb)
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
      eventTrigger(subject = NounPhrase.text("you"), verb = discount.predicate),
      discount.categoryNoun,
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

private fun Describers.renderEvent(trigger: Trigger): Event? {
  if (trigger is ByTrigger) {
    if (trigger.by != anyoneExpression) return null
    val expression = (trigger.inner as? OnGainOf)?.expression ?: return null
    relationshipEvent(expression, Event.ActorConstraint.UNRESTRICTED)?.let {
      return it
    }
    placementEvent(expression, Event.ActorConstraint.UNRESTRICTED)?.let {
      return it
    }
    if (!expression.simple) return null
    return scaleFrame(expression.className)?.let {
      Event(
          Event.Kind.RAISE,
          Event.ActorConstraint.UNRESTRICTED,
          NounPhrase.text(it.subject),
          listOf(Modifier.Phrase("1 step")),
      )
    }
  }
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.complement) return null
  unrestrictedPlayedTagEvent(expression)?.let {
    return it
  }
  relationshipEvent(expression, Event.ActorConstraint.YOU)?.let {
    return it
  }
  productionEvent(expression)?.let {
    return it
  }
  if (expression.refinement == null) {
    scaleFrame(expression.className)?.let {
      return Event(
          Event.Kind.RAISE,
          Event.ActorConstraint.YOU,
          NounPhrase.text(it.subject),
          listOf(Modifier.Phrase("1 step")),
      )
    }
  }
  purchaseEvent(expression)?.let {
    return it
  }
  playedCardEvent(expression)?.let {
    return it
  }
  if (expression.refinement != null) return null
  when (val frame = triggerFrame(expression.className)) {
    is TriggerFrame.PlayCard -> {
      if (expression.simple) {
        return Event(
            Event.Kind.PLAY,
            Event.ActorConstraint.YOU,
            NounPhrase("card", determiner = "a"),
        )
      }
      val represented = representedExpression(expression) ?: return null
      return playedCardEvent(represented)
    }
    is TriggerFrame.PlayTag -> {
      if (frame.phrase == null) {
        val tag = representedClass(expression) ?: return null
        val name = tagName(tag.className)?.first ?: return null
        return Event(
            Event.Kind.PLAY,
            Event.ActorConstraint.YOU,
            NounPhrase("$name tag", determiner = indefiniteArticle(name)),
        )
      }
    }
    else -> Unit
  }
  playedTagPhrase(expression.className)?.let {
    val resolved = resolveExpression(expression) ?: return null
    if (resolved.sourceDependencies.isNotEmpty() || expression.refinement != null) return null
    return Event(Event.Kind.PLAY, Event.ActorConstraint.YOU, NounPhrase.text(it))
  }
  actionUseEvent(trigger)?.let { actionUse ->
    val action = actionUse.provider
    return Event(
        Event.Kind.USE_ACTION,
        Event.ActorConstraint.YOU,
        NounPhrase.text(
            if (action == thisExpression) "this action" else renderActionUse(action) ?: return null
        ),
    )
  }
  val resolvedCardResource = resolveCardResource(expression)
  if (resolvedCardResource != null && cardResourceHasHolder(resolvedCardResource, thisExpression)) {
    cardResourceNoun(expression.className, 1)?.let {
      return Event(
          Event.Kind.ADD,
          Event.ActorConstraint.YOU,
          NounPhrase(it, determiner = indefiniteArticle(it)),
          listOf(Modifier.Phrase("to this card")),
      )
    }
  }
  if (resolvePlacementExpression(expression, this)?.owner == anyoneExpression) {
    placementEvent(expression, Event.ActorConstraint.UNRESTRICTED)?.let {
      return it
    }
  }
  placementEvent(expression, Event.ActorConstraint.YOU)?.let {
    return it
  }
  val resolved = resolveExpression(expression)
  if (resolved?.sourceDependencies?.isEmpty() == true && expression.refinement == null) {
    tagName(expression.className)?.let { (name) ->
      return Event(
          Event.Kind.PLAY,
          Event.ActorConstraint.YOU,
          NounPhrase("$name tag", determiner = indefiniteArticle(name)),
      )
    }
    cardResourceNoun(expression.className, 1)?.let {
      return Event(
          Event.Kind.ADD,
          Event.ActorConstraint.YOU,
          NounPhrase(it, determiner = indefiniteArticle(it)),
          listOf(Modifier.Phrase("to any card")),
      )
    }
  }
  if (resolved?.hasOnlySourceDependency(Key(OWNED, 0), anyoneExpression) == true) {
    tagName(expression.className)?.let { (name) ->
      return Event(
          Event.Kind.PLAY,
          Event.ActorConstraint.UNRESTRICTED,
          NounPhrase("$name tag", determiner = "any"),
      )
    }
  }
  return null
}

private fun Describers.unrestrictedPlayedTagEvent(expression: Expression): Event? {
  if (expression.refinement != null || expression.complement) return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val holderKey = Key(ClassName.cn("Tag"), 0)
  val holder = resolved.sourceDependency(holderKey) ?: return null
  if (
      resolved.sourceDependency(ownerKey) != anyoneExpression ||
          resolved.sourceDependencies.keys != setOf(ownerKey, holderKey)
  ) {
    return null
  }
  val resolvedHolder = resolveExpression(holder) ?: return null
  if (
      !resolvedHolder.hasOnlySourceDependency(ownerKey, anyoneExpression) ||
          holder.refinement != null ||
          holder.complement ||
          fact(holder.className, ComponentDescriber::cardResourceHolder) == null
  ) {
    return null
  }
  val (name) = tagName(expression.className) ?: return null
  return Event(
      Event.Kind.PLAY,
      Event.ActorConstraint.UNRESTRICTED,
      NounPhrase("$name tag", determiner = "any"),
  )
}

private fun Describers.relationshipEvent(
    expression: Expression,
    actorConstraint: Event.ActorConstraint,
): Event? {
  if (expression.refinement != null || expression.complement) return null
  val relation = fact(expression.className, ComponentDescriber::spatialRelation) ?: return null
  val noun = relation.eventNoun ?: return null
  val resolved = resolveExpression(expression) ?: return null
  val sourceKey = Key(ClassName.cn("Adjacency"), 0)
  val targetKey = Key(ClassName.cn("Adjacency"), 1)
  if (resolved.sourceDependencies.keys != setOf(sourceKey, targetKey)) return null
  val source =
      resolved.sourceDependency(sourceKey)?.let { relationshipParticipant(it) } ?: return null
  val target =
      resolved.sourceDependency(targetKey)?.let { relationshipParticipant(it) } ?: return null
  return Event(
      Event.Kind.CREATE,
      actorConstraint,
      NounPhrase(noun, determiner = indefiniteArticle(noun))
          .withModifier(Modifier.Between(source, target)),
  )
}

private fun Describers.relationshipParticipant(expression: Expression): NounPhrase? {
  if (expression.refinement != null || expression.complement) return null
  val placement = positionedFrame(expression.className) ?: return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  return when {
    resolved.sourceDependencies.isEmpty() ->
        NounPhrase(placement.singular, determiner = indefiniteArticle(placement.singular))
    resolved.hasOnlySourceDependency(ownerKey, ownerExpression) -> oneOfYour(placement.plural)
    resolved.hasOnlySourceDependency(ownerKey, notOwnerExpression) ->
        NounPhrase(placement.singular, determiner = "an opponent's")
    else -> null
  }
}

private fun Describers.renderActionUse(expression: Expression): String? {
  val resolved = resolveExpression(expression) ?: return null
  if (resolved.sourceDependencies.isNotEmpty() || expression.complement) return null
  val use = fact(expression.className, ComponentDescriber::actionUse) ?: return null
  val refinement = expression.refinement ?: return use.objectPhrase
  if (refinement.forgiving) return null
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  val propertyMetric = minimum.metric as? Property ?: return null
  if (propertyMetric.receiver != null) return null
  val property = use.minimumProperties[propertyMetric.propertyName.value] ?: return null
  if (minimum.target == 1) {
    property.positiveObjectPhrase?.let {
      return it
    }
  }
  val unit = property.unit?.let { " $it" }.orEmpty()
  val article = indefiniteArticle(property.noun)
  return "${use.objectPhrase} with $article ${property.noun} of ${minimum.target}$unit or more"
}

private fun Describers.purchaseEvent(expression: Expression): Event? {
  if (!expression.simple) return null
  val purchase = triggerFrame(expression.className) as? TriggerFrame.Purchase ?: return null
  val noun = purchase.noun.singular
  return Event(
      Event.Kind.BUY,
      Event.ActorConstraint.YOU,
      NounPhrase(noun, determiner = indefiniteArticle(noun)),
  )
}

private fun Describers.productionEvent(expression: Expression): Event? {
  val production = productionCategoryExpression(expression, this) ?: return null
  if (production.owner != null) return null
  val objectPhrase =
      if (concrete(production.resource)) {
        NounPhrase("${componentNoun(production.resource, 1)} production", determiner = "your")
      } else {
        oneOfYour("productions")
      }
  return Event(
      Event.Kind.INCREASE_PRODUCTION,
      Event.ActorConstraint.YOU,
      objectPhrase,
      listOf(Modifier.Phrase("1 step")),
  )
}

private fun Describers.playedCardEvent(expression: Expression): Event? {
  val description = triggerFrame(expression.className) as? TriggerFrame.PlayCard ?: return null
  val resolved = resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val actorConstraint =
      when {
        resolved.sourceDependencies.isEmpty() -> Event.ActorConstraint.YOU
        resolved.hasOnlySourceDependency(ownerKey, anyoneExpression) ->
            Event.ActorConstraint.UNRESTRICTED
        else -> return null
      }
  val card = componentNoun(expression.className, 1)
  val article =
      if (actorConstraint == Event.ActorConstraint.UNRESTRICTED) "any" else indefiniteArticle(card)
  val cardPhrase = NounPhrase(card, determiner = article)
  val objectPhrase =
      expression.refinement?.let { refinement ->
        if (refinement.forgiving) return null
        val counting = refinement.requirement as? Requirement.Counting ?: return null
        when (val metric = counting.metric) {
          is Metric.Count -> {
            val tagExpression = countedExpression(counting) ?: return null
            if (
                tagExpression.refinement != null ||
                    tagExpression.complement ||
                    resolveExpression(tagExpression)?.let { tag ->
                      tag.sourceDependencies.isNotEmpty() &&
                          !tag.hasOnlySourceDependency(ownerKey, anyoneExpression)
                    } != false
            ) {
              return null
            }
            val tag = tagName(tagExpression.className)?.first
            if (counting is Requirement.Min && counting.target == 1 && tag != null) {
              NounPhrase(
                  "$tag $card",
                  determiner =
                      if (actorConstraint == Event.ActorConstraint.UNRESTRICTED) "any"
                      else indefiniteArticle(tag),
              )
            } else {
              if (
                  !isTag(tagExpression.className) ||
                      (tag == null && playedTagPhrase(tagExpression.className) != null)
              ) {
                return null
              }
              val singular = tag?.let { "$it tag" } ?: "tag"
              val plural = tag?.let { "$it tags" } ?: "tags"
              val quantity =
                  when (counting) {
                    is Requirement.Min -> "${counting.target} or more $plural"
                    is Requirement.Max ->
                        if (counting.target == 0) "no $plural"
                        else
                            "at most ${counting.target} ${if (counting.target == 1) singular else plural}"
                    is Requirement.Exact ->
                        "exactly ${counting.target} ${if (counting.target == 1) singular else plural}"
                  }
              cardPhrase.withModifier(Modifier.Relation("with", NounPhrase.text(quantity)))
            }
          }
          is Property -> {
            val minimum = counting as? Requirement.Min ?: return null
            if (metric.receiver != null) return null
            val property = description.minimumProperties[metric.propertyName.value] ?: return null
            when (property) {
              is ComponentDescriber.MinimumProperty.Threshold -> {
                val unit = property.unit?.let { " $it" }.orEmpty()
                val propertyArticle = indefiniteArticle(property.noun)
                cardPhrase.withModifier(
                    Modifier.Relation(
                        "with",
                        NounPhrase.text(
                            "$propertyArticle ${property.noun} of ${minimum.target}$unit or more"
                        ),
                    )
                )
              }
              is ComponentDescriber.MinimumProperty.Presence -> {
                if (minimum.target != 1) return null
                val propertyArticle = indefiniteArticle(property.noun)
                cardPhrase.withModifier(
                    Modifier.Relation(
                        "with",
                        NounPhrase(property.noun, determiner = propertyArticle),
                    )
                )
              }
            }
          }
          else -> return null
        }
      } ?: cardPhrase
  return Event(Event.Kind.PLAY, actorConstraint, objectPhrase)
}

private fun Describers.placementEvent(
    expression: Expression,
    actorConstraint: Event.ActorConstraint,
): Event? {
  if (expression.refinement != null || expression.complement) return null
  val resolvedPlacement = resolvePlacementExpression(expression, this) ?: return null
  if (resolvedPlacement.unknownDependencies.isNotEmpty()) return null
  if (actorConstraint == Event.ActorConstraint.YOU && resolvedPlacement.owner != null) return null
  if (
      actorConstraint == Event.ActorConstraint.UNRESTRICTED &&
          resolvedPlacement.owner != null &&
          resolvedPlacement.owner != anyoneExpression
  ) {
    return null
  }
  val placement = positionedFrame(expression.className) ?: return null
  val location =
      resolvedPlacement.sites
          .singleOrNull()
          ?.takeIf { it.simple }
          ?.let { fact(it.className, ComponentDescriber::metricLocation) }
  val complements =
      location?.let { listOf(Modifier.Phrase(it)) }
          ?: renderPlacementSites(resolvedPlacement, this)
          ?: return null
  val objectPhrase =
      when (actorConstraint) {
        Event.ActorConstraint.YOU ->
            NounPhrase(placement.singular, placement.plural, determiner = placement.article)
        Event.ActorConstraint.UNRESTRICTED ->
            NounPhrase(placement.singular, placement.plural, determiner = "any")
      }
  return Event(Event.Kind.PLACE, actorConstraint, objectPhrase, complements)
}

private fun oneOfYour(pluralNoun: String): NounPhrase =
    NounPhrase.text("one")
        .withModifier(
            Modifier.Relation(
                "of",
                NounPhrase(pluralNoun, determiner = "your"),
            )
        )

private fun Describers.renderScoringCondition(requirement: Requirement): String? {
  val minimum = requirement as? Requirement.Min ?: return null
  val metric = minimum.metric as? Metric.Count ?: return null
  val expression = metric.expression
  val resolved = resolveCardResource(expression) ?: return null
  if (
      !cardResourceHasHolder(resolved, thisExpression) ||
          expression.refinement != null ||
          expression.complement
  )
      return null
  val noun = cardResourceNoun(expression.className, maxOf(2, minimum.target)) ?: return null
  return "if you have ${minimum.target} or more $noun on this card"
}

private fun Describers.isEndTrigger(expression: Expression): Boolean =
    expression.simple && isEndTrigger(expression.className)

private fun Describers.renderFixedScore(instruction: InstructionTree): String? {
  val (className, count, penalty) =
      when (instruction) {
        is Gain -> {
          if (instruction.intensity.modality() != Modality.REQUIRED) return null
          if (!instruction.gaining.simple) return null
          Triple(
              instruction.gaining.className,
              instruction.count.fixedQuantity() ?: return null,
              false,
          )
        }
        is Remove -> {
          if (instruction.intensity.modality() != Modality.REQUIRED) return null
          if (!instruction.removing.simple) return null
          Triple(
              instruction.removing.className,
              instruction.count.fixedQuantity() ?: return null,
              true,
          )
        }
        else -> return null
      }
  val score = fact(className, ComponentDescriber::score) ?: return null
  return "${if (penalty) "-" else ""}$count ${if (count == 1) score.singular else score.plural}"
}

private fun renderTriggeredInstructions(
    effect: Effect,
    describers: Describers,
): String? {
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
  val result = renderInstructions(instruction, describers)
  return completeSentence("when ${trigger.linearize()}, ${result.asCoordinatedClause()}")
}

private fun renderLinkedProductionReward(effect: Effect, describers: Describers): String? {
  val expression = (effect.trigger as? OnGainOf)?.expression ?: return null
  val production = productionCategoryExpression(expression, describers) ?: return null
  if (production.owner != null || describers.concrete(production.resource)) return null
  val gain = effect.instruction as? Gain ?: return null
  if (gain.intensity.modality() != Modality.REQUIRED) return null
  if (!gain.gaining.simple || gain.gaining.className != production.resource) return null
  val count = gain.count.fixedQuantity() ?: return null
  val objectPhrase = "$count ${if (count == 1) "resource" else "resources"} of that type"
  val result = Clause.Simple(Predicate("gain", Coordination.one(NounPhrase.text(objectPhrase))))
  val trigger =
      eventTrigger(
          subject = NounPhrase.text("you"),
          verb = "increase",
          objectPhrase = NounPhrase.text("one of your productions one or more steps"),
      )
  return Sentence(
          Clause.Prefaced(
              Clause.Preface.Temporal(trigger),
              result.withModifier(Modifier.Phrase("per step")),
          ),
      )
      .linearize()
}

internal fun renderEndEffect(effect: Effect, describers: Describers): String? {
  val condition =
      when (val trigger = effect.trigger) {
        is IfTrigger -> {
          if (!isEndTrigger(trigger.inner, describers)) return null
          describers.renderScoringCondition(trigger.condition) ?: return null
        }
        else -> {
          if (!isEndTrigger(trigger, describers)) return null
          null
        }
      }
  renderPerVictoryPoints(effect.instruction, describers)?.let {
    if (condition != null) return null
    return it
  }
  val points = describers.renderFixedScore(effect.instruction) ?: return null
  return "$points${condition?.let { " $it" } ?: ""}."
}

internal fun isEndEffect(effect: Effect, describers: Describers): Boolean {
  return isEndTrigger(effect.trigger, describers)
}

internal fun isUnconditionalFixedScore(effect: Effect, describers: Describers): Boolean =
    effect.trigger !is IfTrigger &&
        isEndEffect(effect, describers) &&
        describers.renderFixedScore(effect.instruction) != null

private fun isEndTrigger(trigger: Trigger, describers: Describers): Boolean =
    when (trigger) {
      is OnGainOf -> describers.isEndTrigger(trigger.expression)
      is Trigger.Or -> trigger.triggers.all { isEndTrigger(it, describers) }
      is Trigger.WrappingTrigger -> isEndTrigger(trigger.inner, describers)
      is Trigger.OnRemoveOf,
      Trigger.WhenGain,
      Trigger.WhenRemove -> false
    }

private fun renderPerVictoryPoints(
    instruction: InstructionTree,
    describers: Describers,
): String? {
  val per = instruction as? Per ?: return null
  val points = describers.renderFixedScore(per.inner) ?: return null
  val metric = renderMetricPhrase(per.metric, describers) ?: NounPhrase.text("[${per.metric}]")
  return "$points ${Modifier.Per(metric).linearize()}."
}
