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
      renderRemovalPrevention(effect, describers)
          ?: paymentDiscount(effect, describers)?.let { renderPaymentDiscount(listOf(it)) }
          ?: renderResourcePaymentValue(effect, describers)
          ?: renderTriggeredInstructions(effect, describers)
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
              it?.reduction == discount.reduction
            }
    sentences += renderPaymentDiscount(run.filterNotNull())
    index += run.size
  }
  return sentences.joinToString(" ")
}

private fun paymentDiscount(effect: Effect, describers: Describers): PaymentDiscount? {
  owedReduction(effect.instruction, describers)?.let { reduction ->
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
  val reduction = owedReduction(effect.instruction, describers) ?: return null
  return "Each $spent you spend is worth ${reduction.count} ${reduction.noun} extra."
}

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
  val actor = events.map { it.actor }.distinct().singleOrNull() ?: return null
  val objects =
      events
          .map { it.objectPhrase }
          .let {
            if (it.size == 1) it.single() else englishAlternatives(it)
          }
  return kind.renderTrigger(actor, objects)
}

private fun Describers.renderSpentResource(trigger: Trigger): String? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  if (fact(expression.className, ComponentDescriber::spentResourceTrigger) != true) return null
  val resource = representedClass(expression) ?: return null
  return plainGainCategoryNoun(resource.className, 1)
}

private fun Describers.renderActionPaymentDiscountTrigger(trigger: Trigger): EventTrigger? {
  val expression = (trigger as? OnGainOf)?.expression ?: return null
  if (expression.refinement != null || expression.complement) return null
  if (fact(expression.className, ComponentDescriber::usedActionTrigger) != true) return null
  val action = expression.arguments.singleOrNull()?.takeIf { it.simple } ?: return null
  val predicate =
      fact(action.className, ComponentDescriber::actionUse)?.refundDiscountPredicate ?: return null
  return EventTrigger("you", predicate)
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
      fact(expression.className, ComponentDescriber::playedCard) == true
}

private data class Event(
    val kind: EventKind,
    val actor: EventActor,
    val objectPhrase: String,
)

private enum class EventActor {
  YOU,
  UNRESTRICTED,
}

private enum class EventKind(
    private val activeVerb: String? = null,
    private val activeSuffix: String = "",
    private val passiveVerb: String? = null,
    private val passiveSuffix: String = "",
) {
  PLAY(activeVerb = "play", passiveVerb = "is played"),
  PERFORM_OPERATION(activeVerb = ""),
  USE_ACTION(activeVerb = "use"),
  PLACE(activeVerb = "place", passiveVerb = "is placed"),
  RAISE(passiveVerb = "is raised", passiveSuffix = " 1 step"),
  ADD_TO_CARD(activeVerb = "add", activeSuffix = " to any card"),
  ADD_TO_THIS_CARD(activeVerb = "add", activeSuffix = " to this card"),
  ;

  fun renderTrigger(actor: EventActor, objects: String): EventTrigger? =
      when (actor) {
        EventActor.YOU ->
            activeVerb?.let { EventTrigger("you", "$it $objects$activeSuffix".trimStart()) }
        EventActor.UNRESTRICTED ->
            passiveVerb?.let { EventTrigger(null, "$objects $it$passiveSuffix") }
      }
}

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
  playedCardEvent(expression)?.let {
    return it
  }
  if (expression.refinement != null) return null
  when (fact(expression.className, ComponentDescriber::playTrigger)) {
    ComponentDescriber.PlayTrigger.CARD -> {
      if (!expression.simple) return null
      return Event(EventKind.PLAY, EventActor.YOU, "a card")
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
  if (expression.simple) {
    tagName(expression.className)?.let { (name) ->
      return Event(EventKind.PLAY, EventActor.YOU, "${indefiniteArticle(name)} $name tag")
    }
    placementEvent(expression, EventActor.YOU)?.let {
      return it
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

private fun Describers.playedCardEvent(expression: Expression): Event? {
  if (fact(expression.className, ComponentDescriber::playedCard) != true) return null
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
            if (metric.receiver != null || metric.propertyName.value != "cost") return null
            "$article $card with a printed cost of ${minimum.target} M€ or more"
          }
          else -> return null
        }
      } ?: "$article $card"
  return Event(EventKind.PLAY, actor, phrase)
}

private fun Describers.placementEvent(expression: Expression, actor: EventActor): Event? {
  if (expression.refinement != null || expression.complement) return null
  if (
      actor == EventActor.UNRESTRICTED &&
          !expression.simple &&
          expression.arguments != listOf(anyoneExpression)
  )
      return null
  val placement = fact(expression.className, ComponentDescriber::placement) ?: return null
  val phrase =
      when (actor) {
        EventActor.YOU -> "${placement.article} ${placement.singular}"
        EventActor.UNRESTRICTED -> "any ${placement.singular}"
      }
  return Event(EventKind.PLACE, actor, phrase)
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
