package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal fun renderEffect(effect: Effect, describers: Describers): String? =
    if (isEndEffect(effect, describers)) {
      renderEndEffect(effect, describers)
    } else {
      paymentDiscount(effect, describers)?.let { renderPaymentDiscount(listOf(it)) }
          ?: renderResourcePaymentValue(effect, describers)
          ?: renderTriggeredInstructions(effect, describers)
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
              it?.reduction == discount.reduction
            }
    sentences += renderPaymentDiscount(run.filterNotNull())
    index += run.size
  }
  return sentences.joinToString(" ")
}

private fun paymentDiscount(effect: Effect, describers: Describers): PaymentDiscount? {
  describers.renderOwedReduction(effect.instruction)?.let { reduction ->
    val actionTrigger = describers.renderActionPaymentDiscountTrigger(effect.trigger)
    val trigger = actionTrigger ?: describers.renderEventTrigger(effect.trigger) ?: return null
    return PaymentDiscount(
        trigger,
        reduction,
        actionTrigger != null || describers.paymentDiscountRefersToPlayedObject(effect.trigger),
    )
  }
  val trigger = describers.renderActionPaymentDiscountTrigger(effect.trigger) ?: return null
  val reduction = describers.renderPlainGainAmount(effect.instruction) ?: return null
  return PaymentDiscount(trigger, reduction, refersToObject = true)
}

private fun renderPaymentDiscount(discounts: List<PaymentDiscount>): String {
  val clauses = discounts.map { it.trigger }
  fun joinAlternatives(parts: List<String>): String =
      if (parts.size == 1) parts.single() else englishAlternatives(parts)
  val trigger =
      if (clauses.all { it.subject == "you" }) {
        "you " + joinAlternatives(clauses.map { it.predicate })
      } else {
        joinAlternatives(clauses.map(EventTrigger::linearize))
      }
  val reduction = discounts.first().reduction
  val objectReference = if (discounts.all { it.refersToObject }) " for it" else ""
  return completeSentence(
      "when $trigger, you pay ${reduction.count} ${reduction.noun} less$objectReference"
  )
}

private data class PaymentDiscount(
    val trigger: EventTrigger,
    val reduction: ResourceAmount,
    val refersToObject: Boolean,
)

private fun renderResourcePaymentValue(effect: Effect, describers: Describers): String? {
  val spent = describers.renderSpentResource(effect.trigger) ?: return null
  val reduction = describers.renderOwedReduction(effect.instruction) ?: return null
  return "Each $spent you spend is worth ${reduction.count} ${reduction.noun} extra."
}

private data class ResourceAmount(val count: Int, val noun: String)

private data class EventTrigger(val subject: String?, val predicate: String) {
  fun linearize(): String = listOfNotNull(subject, predicate).joinToString(" ")

  fun asCondition(): String = "when ${linearize()}"
}

private fun Describers.renderEventTrigger(trigger: Trigger): EventTrigger? {
  val events =
      when (trigger) {
        is Trigger.Or -> trigger.triggers.map { renderEvent(it) ?: return null }
        else -> listOf(renderEvent(trigger) ?: return null)
      }
  val kind = events.map { it.kind }.distinct().singleOrNull() ?: return null
  val objects =
      events
          .map { it.objectPhrase }
          .let {
            if (it.size == 1) it.single() else englishAlternatives(it)
          }
  return when (kind) {
    EventKind.PLAY -> EventTrigger("you", "play $objects")
    EventKind.PLAY_ANY -> EventTrigger(null, "$objects is played")
    EventKind.USE_ACTION -> EventTrigger("you", "use $objects")
    EventKind.PLACE -> EventTrigger("you", "place $objects")
    EventKind.PLACE_ANY -> EventTrigger(null, "$objects is placed")
    EventKind.RAISE_ANY -> EventTrigger(null, "$objects is raised 1 step")
    EventKind.ADD_TO_CARD -> EventTrigger("you", "add $objects to any card")
  }
}

private fun Describers.renderSpentResource(trigger: Trigger): String? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  if (this[expression.className].spentResourceTrigger != true) return null
  val resource = representedClass(expression) ?: return null
  return plainGainCategoryNoun(resource.className, 1)
}

private fun Describers.renderActionPaymentDiscountTrigger(trigger: Trigger): EventTrigger? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  if (this[expression.className].usedActionTrigger != true) return null
  val action = expression.arguments.singleOrNull()?.takeIf { it.simple } ?: return null
  val predicate = this[action.className].actionUse?.refundDiscountPredicate ?: return null
  return EventTrigger("you", predicate)
}

private fun Describers.renderPlainGainAmount(instruction: InstructionTree): ResourceAmount? {
  val change = instruction as? Instruction ?: return null
  val (className, count) = standardResourceGain(change, this) ?: return null
  return ResourceAmount(count, componentNoun(className, count))
}

private fun Describers.paymentDiscountRefersToPlayedObject(trigger: Trigger): Boolean {
  val expression = (trigger as? OnGainOf)?.expression ?: return false
  return this[expression.className].playTrigger == ComponentDescriber.PlayTrigger.CARD ||
      this[expression.className].playedCard == true
}

private fun Describers.renderOwedReduction(instruction: InstructionTree): ResourceAmount? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity != null && removal.intensity != MANDATORY) return null
  val expression = removal.removing
  if (expression.refinement != null || expression.complement) return null
  if (this[expression.className].owedPayment != true) return null
  val resource = representedClass(expression) ?: return null
  val count = (removal.count as? ActualScalar)?.value ?: return null
  return ResourceAmount(count, plainGainNoun(resource.className, count) ?: return null)
}

private data class Event(val kind: EventKind, val objectPhrase: String)

private enum class EventKind {
  PLAY,
  PLAY_ANY,
  USE_ACTION,
  PLACE,
  PLACE_ANY,
  RAISE_ANY,
  ADD_TO_CARD,
}

private fun Describers.renderEvent(trigger: Trigger): Event? {
  if (trigger is ByTrigger) {
    if (trigger.by != anyoneExpression) return null
    val expression = (trigger.inner as? OnGainOf)?.expression ?: return null
    placementEvent(expression, EventKind.PLACE_ANY)?.let {
      return it
    }
    if (!expression.simple) return null
    return this[expression.className].track?.let { Event(EventKind.RAISE_ANY, it.subject) }
  }
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.complement) return null
  playedCardEvent(expression)?.let {
    return it
  }
  if (expression.refinement != null) return null
  when (this[expression.className].playTrigger) {
    ComponentDescriber.PlayTrigger.CARD -> {
      if (!expression.simple) return null
      return Event(EventKind.PLAY, "a card")
    }
    ComponentDescriber.PlayTrigger.TAG -> {
      val tag = representedClass(expression) ?: return null
      val name = tagName(tag.className)?.first ?: return null
      return Event(EventKind.PLAY, "${indefiniteArticle(name)} $name tag")
    }
    null -> Unit
  }
  this[expression.className].playedTagPhrase?.let {
    if (!expression.simple) return null
    return Event(EventKind.PLAY, it)
  }
  if (this[expression.className].usedActionTrigger == true) {
    val action = expression.arguments.singleOrNull()?.takeIf { it.simple } ?: return null
    return Event(
        EventKind.USE_ACTION,
        this[action.className].actionUse?.objectPhrase ?: return null,
    )
  }
  if (expression.simple) {
    tagName(expression.className)?.let { (name) ->
      return Event(EventKind.PLAY, "${indefiniteArticle(name)} $name tag")
    }
    placementEvent(expression, EventKind.PLACE)?.let {
      return it
    }
    cardResourceNoun(expression.className, 1)?.let {
      return Event(EventKind.ADD_TO_CARD, "${indefiniteArticle(it)} $it")
    }
  }
  if (expression.arguments == listOf(anyoneExpression)) {
    tagName(expression.className)?.let { (name) ->
      return Event(EventKind.PLAY_ANY, "any $name tag")
    }
    placementEvent(expression, EventKind.PLACE_ANY)?.let {
      return it
    }
  }
  return null
}

private fun Describers.playedCardEvent(expression: Expression): Event? {
  if (expression.arguments.isNotEmpty() || this[expression.className].playedCard != true)
      return null
  val card = componentNoun(expression.className, 1)
  val phrase =
      expression.refinement?.let { refinement ->
        if (refinement.forgiving) return null
        val minimum = refinement.requirement as? Requirement.Min ?: return null
        val qualifier =
            when (val metric = minimum.metric) {
              is Metric.Count -> {
                if (minimum.target != 1) return null
                val tag = tagName(minimum)?.first ?: return null
                "with ${indefiniteArticle(tag)} $tag tag"
              }
              is Property -> {
                if (metric.receiver != null || metric.propertyName.value != "cost") return null
                "with a printed cost of ${minimum.target} M€ or more"
              }
              else -> return null
            }
        "${indefiniteArticle(card)} $card $qualifier"
      } ?: "${indefiniteArticle(card)} $card"
  return Event(EventKind.PLAY, phrase)
}

private fun Describers.placementEvent(expression: Expression, kind: EventKind): Event? {
  if (expression.refinement != null || expression.complement) return null
  if (
      kind == EventKind.PLACE_ANY &&
          !expression.simple &&
          expression.arguments != listOf(anyoneExpression)
  )
      return null
  val placement = this[expression.className].placement ?: return null
  val phrase =
      when (kind) {
        EventKind.PLACE -> "${placement.article} ${placement.singular}"
        EventKind.PLACE_ANY -> "any ${placement.singular}"
        else -> return null
      }
  return Event(kind, phrase)
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
    expression.simple && this[expression.className].endTrigger == true

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
  val score = this[className].score ?: return null
  return "${if (penalty) "-" else ""}$count ${if (count == 1) score.singular else score.plural}"
}

private fun renderTriggeredInstructions(effect: Effect, describers: Describers): String? {
  val trigger = describers.renderEventTrigger(effect.trigger) ?: return null
  val result = renderInstructions(effect.instruction, describers = describers) ?: return null
  return completeSentence("${trigger.asCondition()}, ${result.asCoordinatedClause()}")
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
