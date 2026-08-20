package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal fun renderEffect(effect: Effect, describers: Describers): String? {
  val lowered = lowerProductionSyntax(effect)
  return if (isEndEffect(lowered, describers)) {
    renderEndEffect(lowered, describers)
  } else {
    renderRemovalPrevention(lowered, describers)
        ?: renderPurchaseAdjustment(lowered, describers)
        ?: paymentDiscount(lowered, describers)?.let { renderPaymentDiscount(listOf(it)) }
        ?: renderResourcePaymentValue(lowered, describers)
        ?: renderRequirementFlexibility(lowered, describers)
        ?: renderLinkedProductionReward(lowered, describers)
        ?: renderTriggeredInstructions(lowered, describers)
  }
}

private fun renderRequirementFlexibility(effect: Effect, describers: Describers): String? {
  val trigger = effect.trigger as? OnGainOf ?: return null
  if (
      !trigger.expression.simple ||
          describers.fact(trigger.expression.className, ComponentDescriber::playTrigger) !=
              ComponentDescriber.PlayTrigger.CARD
  ) {
    return null
  }
  val removal = effect.instruction as? Remove ?: return null
  if (
      removal.intensity != AMAP ||
          removal.removing.refinement != null ||
          removal.removing.complement ||
          describers.fact(
              removal.removing.className,
              ComponentDescriber::requirementShortfall,
          ) != true
  ) {
    return null
  }
  val count = (removal.count as? ActualScalar)?.value ?: return null
  val target = describers.representedClass(removal.removing) ?: return null
  val requirementKind =
      describers.fact(target.className, ComponentDescriber::track)?.subject
          ?: describers.fact(target.className, ComponentDescriber::requirementKind)
          ?: return null
  val steps = if (count == 1) "step" else "steps"
  return completeSentence(
      "when you play a card, you may treat its $requirementKind requirement as if it were " +
          "$count $steps lower or higher"
  )
}

private fun renderPurchaseAdjustment(effect: Effect, describers: Describers): String? {
  val trigger = effect.trigger as? OnGainOf ?: return null
  if (!trigger.expression.simple) return null
  if (describers.fact(trigger.expression.className, ComponentDescriber::purchase) == null)
      return null
  val triggerClause = describers.renderEventTrigger(trigger) ?: return null
  val change = effect.instruction as? Instruction.Change ?: return null
  if (change.intensity != null && change.intensity != MANDATORY) return null
  val expression = change.gaining ?: change.removing ?: return null
  if (!expression.simple || !describers.concrete(expression.className)) return null
  if (describers.fact(expression.className, ComponentDescriber::standardResource) != true) {
    return null
  }
  val adjustment = (change.count as? ActualScalar)?.value ?: return null
  val direction =
      when (change) {
        is Gain -> "less"
        is Remove -> "extra"
        is Instruction.Transmute -> return null
      }
  val resource = describers.componentNoun(expression.className, adjustment)
  return Sentence(
          Clause.Prefaced(
              "when ${triggerClause.linearize()}",
              Clause.Simple(
                  predicate =
                      Predicate(
                          "pay",
                          Coordination.one(NounPhrase.text("$adjustment $resource $direction")),
                      ),
              ),
          )
      )
      .linearize()
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
          it.arguments == listOf(describers.thisExpression)
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
  if (gain.intensity != null && gain.intensity != MANDATORY) return false
  return gain.gaining.simple &&
      describers.concrete(gain.gaining.className) &&
      describers.fact(gain.gaining.className, ComponentDescriber::deadEndSignal) == true &&
      (gain.count as? ActualScalar)?.value == 1
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
  if (describers.fact(expression.className, ComponentDescriber::standardResource) != true) {
    return null
  }
  return describers.componentNoun(expression.className, 2)
}

internal fun renderEffects(effects: List<Effect>, describers: Describers): String? {
  val sentences = mutableListOf<String>()
  var index = 0
  while (index < effects.size) {
    val discount = paymentDiscount(effects[index], describers)
    if (discount == null) {
      sentences += renderEffect(effects[index], describers) ?: return null
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
  return sentences.joinToString(" ")
}

private fun paymentDiscount(effect: Effect, describers: Describers): PaymentDiscount? {
  owedReduction(effect.instruction, describers)?.let { reduction ->
    val actionTrigger = describers.renderActionPaymentDiscountTrigger(effect.trigger)
    val trigger =
        actionTrigger?.clause ?: describers.renderEventTrigger(effect.trigger) ?: return null
    return PaymentDiscount(
        trigger,
        reduction,
        actionTrigger != null || describers.paymentDiscountRefersToPlayedObject(effect.trigger),
    )
  }
  val trigger = describers.renderActionPaymentDiscountTrigger(effect.trigger) ?: return null
  val actualReduction = describers.renderPlainGainAmount(effect.instruction) ?: return null
  val reduction =
      trigger.refundDiscountNoun?.let { noun ->
        ResourceAmount(
            actualReduction.count,
            if (actualReduction.count == 1) noun.singular else noun.plural,
        )
      } ?: actualReduction
  return PaymentDiscount(
      trigger.clause,
      reduction,
      refersToObject = true,
      categoryReduction = trigger.refundDiscountNoun != null,
  )
}

private fun renderPaymentDiscount(discounts: List<PaymentDiscount>): String {
  val clauses = discounts.map { it.trigger }.distinct()
  fun joinAlternatives(parts: List<String>): String =
      if (parts.size == 1) parts.single() else englishAlternatives(parts)
  val actingPlayer = NounPhrase.text("you")
  val trigger =
      if (clauses.all { it.subject == actingPlayer }) {
        actingPlayer.linearize() + " " + joinAlternatives(clauses.map { it.predicate.linearize() })
      } else {
        joinAlternatives(clauses.map(Clause.Simple::linearize))
      }
  val reduction = discounts.first().reduction
  val objectReference = if (discounts.all { it.refersToObject }) " for it" else ""
  val reductionPhrase =
      if (discounts.first().categoryReduction) {
        "${reduction.count} less ${reduction.noun}"
      } else {
        "${reduction.count} ${reduction.noun} less"
      }
  return completeSentence("when $trigger, you pay $reductionPhrase$objectReference")
}

private data class PaymentDiscount(
    val trigger: Clause.Simple,
    val reduction: ResourceAmount,
    val refersToObject: Boolean,
    val categoryReduction: Boolean = false,
)

private fun renderResourcePaymentValue(effect: Effect, describers: Describers): String? {
  val spent = describers.renderSpentResource(effect.trigger) ?: return null
  val reduction = owedReduction(effect.instruction, describers) ?: return null
  return "Each $spent you spend is worth ${reduction.count} ${reduction.noun} extra."
}

private fun Describers.renderEventTrigger(trigger: Trigger): Clause.Simple? {
  val events =
      when (trigger) {
        is Trigger.Or -> trigger.triggers.map { renderEvent(it) ?: return null }
        else -> listOf(renderEvent(trigger) ?: return null)
      }
  val clauses = events.map { it.renderTrigger() ?: return null }
  return if (clauses.size == 1) clauses.single()
  else coordinateClauseObjects(clauses, Conjunction.OR)
}

private fun Describers.renderSpentResource(trigger: Trigger): String? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  if (fact(expression.className, ComponentDescriber::spentResourceTrigger) != true) return null
  val resource = representedClass(expression) ?: return null
  return plainGainCategoryNoun(resource.className, 1)
}

private data class ActionPaymentDiscountTrigger(
    val clause: Clause.Simple,
    val refundDiscountNoun: ComponentDescriber.Noun.Counted?,
)

private fun Describers.renderActionPaymentDiscountTrigger(
    trigger: Trigger,
): ActionPaymentDiscountTrigger? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  if (fact(expression.className, ComponentDescriber::usedActionTrigger) != true) return null
  val action = expression.arguments.singleOrNull()?.takeIf { it.simple } ?: return null
  val use = fact(action.className, ComponentDescriber::actionUse) ?: return null
  val predicate = use.refundDiscountPredicate ?: return null
  return ActionPaymentDiscountTrigger(
      eventTrigger(subject = NounPhrase.text("you"), verb = predicate),
      use.refundDiscountNoun,
  )
}

private fun Describers.renderPlainGainAmount(instruction: InstructionTree): ResourceAmount? {
  val change = instruction as? Instruction ?: return null
  val (className, count) = standardResourceGain(change, this) ?: return null
  return ResourceAmount(count, componentNoun(className, count))
}

private fun Describers.paymentDiscountRefersToPlayedObject(trigger: Trigger): Boolean {
  val expression = (trigger as? OnGainOf)?.expression ?: return false
  return fact(expression.className, ComponentDescriber::playTrigger) ==
      ComponentDescriber.PlayTrigger.CARD ||
      fact(expression.className, ComponentDescriber::playedCard) != null
}

private data class Event(
    val kind: EventKind,
    val actor: EventActor,
    val objectPhrase: NounPhrase,
    val modifiers: List<Modifier> = emptyList(),
) {
  constructor(
      kind: EventKind,
      actor: EventActor,
      objectPhrase: String,
      modifiers: List<Modifier> = emptyList(),
  ) : this(kind, actor, NounPhrase.text(objectPhrase), modifiers)

  fun renderTrigger(): Clause.Simple? = kind.renderTrigger(actor, objectPhrase, modifiers)
}

private enum class EventActor {
  YOU,
  UNRESTRICTED,
}

private enum class EventKind(
    private val activeVerb: String? = null,
    private val activeModifier: String? = null,
    private val passiveVerb: String? = null,
    private val passiveModifier: String? = null,
) {
  PLAY(activeVerb = "play", passiveVerb = "is played"),
  PERFORM_OPERATION(activeVerb = ""),
  BUY(activeVerb = "buy"),
  USE_ACTION(activeVerb = "use"),
  PLACE(activeVerb = "place", passiveVerb = "is placed"),
  INCREASE_PRODUCTION(activeVerb = "increase", activeModifier = "1 step"),
  RAISE(passiveVerb = "is raised", passiveModifier = "1 step"),
  ADD_TO_CARD(activeVerb = "add", activeModifier = "to any card"),
  ADD_TO_THIS_CARD(activeVerb = "add", activeModifier = "to this card"),
  ;

  fun renderTrigger(
      actor: EventActor,
      objectPhrase: NounPhrase,
      modifiers: List<Modifier>,
  ): Clause.Simple? =
      when (actor) {
        EventActor.YOU ->
            activeVerb?.let { verb ->
              eventTrigger(
                  subject = NounPhrase.text("you"),
                  verb = verb,
                  objectPhrase = objectPhrase,
                  modifiers = modifiers + listOfNotNull(activeModifier?.let(Modifier::Phrase)),
              )
            }
        EventActor.UNRESTRICTED ->
            passiveVerb?.let { verb ->
              eventTrigger(
                  subject = objectPhrase,
                  verb = verb,
                  modifiers = modifiers + listOfNotNull(passiveModifier?.let(Modifier::Phrase)),
              )
            }
      }
}

private fun eventTrigger(
    subject: NounPhrase,
    verb: String,
    objectPhrase: NounPhrase? = null,
    modifiers: List<Modifier> = emptyList(),
): Clause.Simple =
    Clause.Simple(
        predicate =
            Predicate(
                verb,
                objectPhrase?.let { Coordination.one(it) },
                modifiers,
            ),
        subject = subject,
    )

private fun Describers.renderEvent(trigger: Trigger): Event? {
  if (trigger is ByTrigger) {
    if (trigger.by != anyoneExpression) return null
    val expression = (trigger.inner as? OnGainOf)?.expression ?: return null
    placementEvent(expression, EventActor.UNRESTRICTED)?.let {
      return it
    }
    if (!expression.simple) return null
    return fact(expression.className, ComponentDescriber::track)?.let {
      Event(EventKind.RAISE, EventActor.UNRESTRICTED, it.subject)
    }
  }
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.complement) return null
  fact(expression.className, ComponentDescriber::operationTrigger)?.let {
    if (expression.refinement == null && expression.arguments.all(Expression::simple)) {
      return Event(EventKind.PERFORM_OPERATION, EventActor.YOU, it)
    }
  }
  productionEvent(expression)?.let {
    return it
  }
  purchaseEvent(expression)?.let {
    return it
  }
  playedCardEvent(expression)?.let {
    return it
  }
  if (expression.refinement != null) return null
  when (fact(expression.className, ComponentDescriber::playTrigger)) {
    ComponentDescriber.PlayTrigger.CARD -> {
      if (expression.simple) return Event(EventKind.PLAY, EventActor.YOU, "a card")
      val represented = representedExpression(expression) ?: return null
      return playedCardEvent(represented)
    }
    ComponentDescriber.PlayTrigger.TAG -> {
      val tag = representedClass(expression) ?: return null
      val name = tagName(tag.className)?.first ?: return null
      return Event(EventKind.PLAY, EventActor.YOU, "${indefiniteArticle(name)} $name tag")
    }
    null -> Unit
  }
  fact(expression.className, ComponentDescriber::playedTagPhrase)?.let {
    if (!expression.simple) return null
    return Event(EventKind.PLAY, EventActor.YOU, it)
  }
  if (fact(expression.className, ComponentDescriber::usedActionTrigger) == true) {
    val action = expression.arguments.singleOrNull()?.takeIf { it.simple } ?: return null
    return Event(
        EventKind.USE_ACTION,
        EventActor.YOU,
        fact(action.className, ComponentDescriber::actionUse)?.objectPhrase ?: return null,
    )
  }
  if (expression.arguments == listOf(thisExpression)) {
    cardResourceNoun(expression.className, 1)?.let {
      return Event(
          EventKind.ADD_TO_THIS_CARD,
          EventActor.YOU,
          "${indefiniteArticle(it)} $it",
      )
    }
  }
  placementEvent(expression, EventActor.YOU)?.let {
    return it
  }
  if (expression.simple) {
    tagName(expression.className)?.let { (name) ->
      return Event(EventKind.PLAY, EventActor.YOU, "${indefiniteArticle(name)} $name tag")
    }
    cardResourceNoun(expression.className, 1)?.let {
      return Event(EventKind.ADD_TO_CARD, EventActor.YOU, "${indefiniteArticle(it)} $it")
    }
  }
  if (expression.arguments == listOf(anyoneExpression)) {
    tagName(expression.className)?.let { (name) ->
      return Event(EventKind.PLAY, EventActor.UNRESTRICTED, "any $name tag")
    }
    placementEvent(expression, EventActor.UNRESTRICTED)?.let {
      return it
    }
  }
  return null
}

private fun Describers.purchaseEvent(expression: Expression): Event? {
  if (!expression.simple) return null
  val purchase = fact(expression.className, ComponentDescriber::purchase) ?: return null
  val noun = purchase.noun.singular
  return Event(
      EventKind.BUY,
      EventActor.YOU,
      "${indefiniteArticle(noun)} $noun",
      listOfNotNull(purchase.destination?.let(Modifier::Phrase)),
  )
}

private fun Describers.productionEvent(expression: Expression): Event? {
  val (owners, resource) = productionCategoryExpression(expression) ?: return null
  if (owners.isNotEmpty()) return null
  val objectPhrase =
      if (concrete(resource)) "your ${componentNoun(resource, 1)} production"
      else "one of your productions"
  return Event(EventKind.INCREASE_PRODUCTION, EventActor.YOU, objectPhrase)
}

private fun Describers.playedCardEvent(expression: Expression): Event? {
  val description = fact(expression.className, ComponentDescriber::playedCard) ?: return null
  val actor =
      when (expression.arguments) {
        emptyList<Expression>() -> EventActor.YOU
        listOf(anyoneExpression) -> EventActor.UNRESTRICTED
        else -> return null
      }
  val card = componentNoun(expression.className, 1)
  val article = if (actor == EventActor.UNRESTRICTED) "any" else indefiniteArticle(card)
  val phrase =
      expression.refinement?.let { refinement ->
        if (refinement.forgiving) return null
        val minimum = refinement.requirement as? Requirement.Min ?: return null
        when (val metric = minimum.metric) {
          is Metric.Count -> {
            if (minimum.target != 1) return null
            val tagExpression = metric.expression
            if (
                tagExpression.refinement != null ||
                    tagExpression.complement ||
                    (tagExpression.arguments.isNotEmpty() &&
                        tagExpression.arguments != listOf(anyoneExpression))
            ) {
              return null
            }
            val tag = tagName(tagExpression.className)?.first ?: return null
            "${if (actor == EventActor.UNRESTRICTED) "any" else indefiniteArticle(tag)} $tag $card"
          }
          is Property -> {
            if (metric.receiver != null) return null
            val property = description.minimumProperties[metric.propertyName.value] ?: return null
            when (property) {
              is ComponentDescriber.PlayedCard.MinimumProperty.Threshold -> {
                val unit = property.unit?.let { " $it" }.orEmpty()
                val propertyArticle = indefiniteArticle(property.noun)
                "$article $card with $propertyArticle ${property.noun} of ${minimum.target}$unit or more"
              }
              is ComponentDescriber.PlayedCard.MinimumProperty.Presence -> {
                if (minimum.target != 1) return null
                val propertyArticle = indefiniteArticle(property.noun)
                "$article $card with $propertyArticle ${property.noun}"
              }
            }
          }
          else -> return null
        }
      } ?: "$article $card"
  return Event(EventKind.PLAY, actor, phrase)
}

private fun Describers.placementEvent(expression: Expression, actor: EventActor): Event? {
  if (expression.refinement != null || expression.complement) return null
  val siteArguments =
      when (actor) {
        EventActor.YOU -> expression.arguments
        EventActor.UNRESTRICTED ->
            when (expression.arguments) {
              emptyList<Expression>(),
              listOf(anyoneExpression) -> emptyList()
              else -> return null
            }
      }
  val placement = fact(expression.className, ComponentDescriber::placement) ?: return null
  val modifiers = renderPlacementSites(siteArguments, this) ?: return null
  val phrase =
      when (actor) {
        EventActor.YOU -> "${placement.article} ${placement.singular}"
        EventActor.UNRESTRICTED -> "any ${placement.singular}"
      }
  return Event(EventKind.PLACE, actor, phrase, modifiers)
}

private fun Describers.renderScoringCondition(requirement: Requirement): String? {
  val minimum = requirement as? Requirement.Min ?: return null
  val metric = minimum.metric as? Metric.Count ?: return null
  val expression = metric.expression
  if (
      expression.arguments != listOf(thisExpression) ||
          expression.refinement != null ||
          expression.complement
  )
      return null
  val noun = cardResourceNoun(expression.className, maxOf(2, minimum.target)) ?: return null
  return "if you have ${minimum.target} or more $noun on this card"
}

private fun Describers.isEndTrigger(expression: Expression): Boolean =
    expression.simple && fact(expression.className, ComponentDescriber::endTrigger) == true

private fun Describers.renderFixedScore(instruction: InstructionTree): String? {
  val (className, count, penalty) =
      when (instruction) {
        is Gain -> {
          if (instruction.intensity != null && instruction.intensity != MANDATORY) return null
          if (!instruction.gaining.simple) return null
          Triple(
              instruction.gaining.className,
              (instruction.count as? ActualScalar)?.value ?: return null,
              false,
          )
        }
        is Remove -> {
          if (instruction.intensity != null && instruction.intensity != MANDATORY) return null
          if (!instruction.removing.simple) return null
          Triple(
              instruction.removing.className,
              (instruction.count as? ActualScalar)?.value ?: return null,
              true,
          )
        }
        else -> return null
      }
  val score = fact(className, ComponentDescriber::score) ?: return null
  return "${if (penalty) "-" else ""}$count ${if (count == 1) score.singular else score.plural}"
}

private fun renderTriggeredInstructions(effect: Effect, describers: Describers): String? {
  val trigger = describers.renderEventTrigger(effect.trigger) ?: return null
  val result = renderInstructions(effect.instruction, describers = describers) ?: return null
  return completeSentence("when ${trigger.linearize()}, ${result.asCoordinatedClause()}")
}

private fun renderLinkedProductionReward(effect: Effect, describers: Describers): String? {
  val expression = (effect.trigger as? OnGainOf)?.expression ?: return null
  val (owners, resource) = describers.productionCategoryExpression(expression) ?: return null
  if (owners.isNotEmpty() || describers.concrete(resource)) return null
  val gain = effect.instruction as? Gain ?: return null
  if (gain.intensity != null && gain.intensity != MANDATORY) return null
  if (!gain.gaining.simple || gain.gaining.className != resource) return null
  val count = (gain.count as? ActualScalar)?.value ?: return null
  val objectPhrase = "$count ${if (count == 1) "resource" else "resources"} of that type"
  val result = Clause.Simple(Predicate("gain", Coordination.one(NounPhrase.text(objectPhrase))))
  val trigger =
      eventTrigger(
          subject = NounPhrase.text("you"),
          verb = "increase",
          objectPhrase = NounPhrase.text("one of your productions"),
      )
  return Sentence(Clause.Prefaced("for each step ${trigger.linearize()}", result)).linearize()
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
  val metric = renderMetricPhrase(per.metric, describers) ?: return null
  return "$points for $metric."
}
