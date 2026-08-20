package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar

/** Interprets one Pets state change from passive component construction facts. */
internal fun renderChange(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val expression =
      when (instruction) {
        is Gain -> instruction.gaining
        is Remove -> instruction.removing
        is Transmute -> instruction.gaining
        else -> return null
      }
  describers.fact(expression.className, ComponentDescriber::directChange)?.let {
    renderDirectChange(instruction, it, describers)?.let { clause ->
      return clause
    }
  }
  if (describers.fact(expression.className, ComponentDescriber::discardable) == true)
      return renderDiscard(instruction, describers)
  if (instruction is Transmute) {
    renderCardResourceDrawExchange(instruction, describers)?.let {
      return it
    }
  }
  if (describers.fact(expression.className, ComponentDescriber::cardResource) != null)
      return renderCardResourceChange(instruction, describers)
  if (describers.fact(expression.className, ComponentDescriber::production) == true)
      return renderProductionChange(instruction, describers)
  describers.fact(expression.className, ComponentDescriber::track)?.let {
    return renderTrackChange(instruction, it)
  }
  describers.fact(expression.className, ComponentDescriber::placement)?.let {
    return renderPlacement(instruction, it, describers)
  }
  if (describers.fact(expression.className, ComponentDescriber::standardResource) == true)
      return renderStandardResourceChange(instruction, describers)
  return null
}

private fun renderDiscard(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity != null && removal.intensity != MANDATORY) return null
  if (!removal.removing.simple) return null
  val count = (removal.count as? ActualScalar)?.value ?: return null
  return clause("discard", describers.componentNounPhrase(removal.removing.className, count))
}

internal fun isProductionChange(instruction: Instruction, describers: Describers): Boolean {
  val expression =
      (instruction as? Instruction.Change)?.let { it.gaining ?: it.removing } ?: return false
  return describers.fact(expression.className, ComponentDescriber::production) == true
}

internal fun isCoalescibleStandardResourceGain(
    instruction: Instruction,
    describers: Describers,
): Boolean {
  val expression = (instruction as? Gain)?.gaining ?: return false
  return describers.fact(expression.className, ComponentDescriber::standardResource) == true
}

internal fun standardResourceGain(
    instruction: Instruction,
    describers: Describers,
): Pair<ClassName, Int>? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  return (className to count).takeIf {
    describers.concrete(className) &&
        describers.fact(className, ComponentDescriber::standardResource) == true
  }
}

private fun renderDirectChange(
    instruction: Instruction,
    description: ComponentDescriber.DirectChange,
    describers: Describers,
): Clause? {
  return when (description) {
    is ComponentDescriber.DirectChange.Gain -> {
      val (_, count) = concreteMandatoryGain(instruction) ?: return null
      if (count != description.count) return null
      clause("gain", NounPhrase(description.noun, count = count))
    }
    is ComponentDescriber.DirectChange.GainChoice -> {
      val gain = instruction as? Gain ?: return null
      if (gain.intensity != null && gain.intensity != MANDATORY) return null
      if (!gain.gaining.simple || describers.concrete(gain.gaining.className)) return null
      if ((gain.count as? ActualScalar)?.value != 1) return null
      clause("gain", NounPhrase.text(description.objectPhrase))
    }
    is ComponentDescriber.DirectChange.Imperative -> {
      val gain = instruction as? Gain ?: return null
      if (gain.intensity != null && gain.intensity != MANDATORY) return null
      if (!gain.gaining.simple || !describers.concrete(gain.gaining.className)) return null
      if ((gain.count as? ActualScalar)?.value != 1) return null
      clause(description.verb, NounPhrase.text(description.objectPhrase))
    }
    ComponentDescriber.DirectChange.NextPlayedCardDiscount ->
        renderNextPlayedCardDiscount(instruction, describers)
    ComponentDescriber.DirectChange.ProductionBoxCopy ->
        renderProductionBoxCopy(instruction, describers)
    ComponentDescriber.DirectChange.FirstAction -> renderFirstAction(instruction, describers)
    ComponentDescriber.DirectChange.TopCardPurchase ->
        renderTopCardPurchase(instruction, describers)
  }
}

private fun renderFirstAction(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  if (count != 1) return null
  val declaration = describers.directChangeSubclassDeclaration(className) ?: return null
  val effect = declaration.effects.singleOrNull() ?: return null
  if (effect.automatic) return null
  val trigger = (effect.trigger as? OnGainOf)?.expression ?: return null
  if (
      trigger.arguments != listOf(describers.thisExpression) ||
          trigger.refinement != null ||
          trigger.complement ||
          describers.fact(trigger.className, ComponentDescriber::actionNumber) != 1
  ) {
    return null
  }
  val result =
      renderInstructions(effect.instruction, describers)?.clauses?.singleOrNull() ?: return null
  return Clause.Prefaced("as your first action", result)
}

private fun renderProductionBoxCopy(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity != null && gain.intensity != MANDATORY) return null
  if (
      !describers.concrete(gain.gaining.className) ||
          gain.gaining.refinement != null ||
          gain.gaining.complement ||
          (gain.count as? ActualScalar)?.value != 1
  ) {
    return null
  }
  val card = gain.gaining.arguments.singleOrNull() ?: return null
  val holder = describers.renderOwnedCardResourceHolder(card) ?: return null
  return clause("duplicate", NounPhrase.text("the production box of $holder"))
}

private fun renderNextPlayedCardDiscount(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  if (count != 1) return null
  val declaration = describers.directChangeSubclassDeclaration(className) ?: return null
  val effect = declaration.effects.singleOrNull() ?: return null
  if (!effect.automatic) return null
  val played = (effect.trigger as? OnGainOf)?.expression ?: return null
  if (
      !played.simple ||
          describers.fact(played.className, ComponentDescriber::playTrigger) !=
              ComponentDescriber.PlayTrigger.CARD
  ) {
    return null
  }
  val reduction = owedReduction(effect.instruction, describers) ?: return null
  return Clause.Simple(
      predicate =
          Predicate(
              "costs",
              Coordination.one(NounPhrase.text("${reduction.count} ${reduction.noun} less")),
          ),
      subject = NounPhrase.text("the next card you play this generation"),
  )
}

private fun renderTopCardPurchase(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val gain = instruction as? Gain ?: return null
  if (
      gain.intensity != OPTIONAL ||
          !gain.gaining.simple ||
          !describers.concrete(gain.gaining.className) ||
          (gain.count as? ActualScalar)?.value != 1
  ) {
    return null
  }
  val look = clause("look at", NounPhrase.text("the top card"))
  val decision =
      Clause.Coordinated(
          Coordination(
              listOf(
                  clause("buy", NounPhrase.text("it")),
                  clause("discard", NounPhrase.text("it")),
              ),
              Conjunction.EITHER_OR,
          )
      )
  return Clause.Coordinated(Coordination(listOf(look, decision), Conjunction.AND))
}

private fun renderCardResourceDrawExchange(
    transmute: Transmute,
    describers: Describers,
): Clause? {
  if (transmute.intensity != null && transmute.intensity != MANDATORY) return null
  val count = (transmute.count as? ActualScalar)?.value ?: return null
  val gaining = transmute.gaining
  if (!gaining.simple || !describers.concrete(gaining.className)) return null
  val removing = transmute.removing
  if (
      removing.arguments != listOf(describers.thisExpression) ||
          removing.refinement != null ||
          removing.complement
  ) {
    return null
  }
  val resource = describers.cardResourceNounPhrase(removing.className, count) ?: return null
  if (describers.fact(gaining.className, ComponentDescriber::draw) != true) return null
  val cards = describers.componentNounPhrase(gaining.className, count)
  return clause(
      "remove",
      resource,
      Modifier.Phrase("from this card"),
      Modifier.Phrase("to draw ${cards.linearize()}"),
  )
}

private fun renderStandardResourceChange(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  standardResourceGain(instruction, describers)?.let { (className, count) ->
    return clause("gain", describers.componentNounPhrase(className, count))
  }
  (instruction as? Transmute)?.let {
    return renderStandardResourceTransfer(it, describers)
  }
  val removal = instruction as? Remove ?: return null
  val expression = removal.removing
  if (expression.refinement != null || expression.complement) return null
  val count = (removal.count as? ActualScalar)?.value ?: return null
  if (!describers.concrete(expression.className)) return null
  if (describers.fact(expression.className, ComponentDescriber::standardResource) != true)
      return null
  if (expression.simple && (removal.intensity == null || removal.intensity == MANDATORY)) {
    return clause("remove", describers.componentNounPhrase(expression.className, count))
  }
  if (
      expression.arguments == listOf(describers.anyoneExpression) && removal.intensity == OPTIONAL
  ) {
    val noun = describers.componentNoun(expression.className, count)
    return clause(
        "remove",
        NounPhrase.text("up to $count $noun"),
        Modifier.Phrase("from any player"),
    )
  }
  return null
}

private fun renderStandardResourceTransfer(
    transmute: Transmute,
    describers: Describers,
): Clause? {
  if (transmute.intensity != OPTIONAL) return null
  val gaining = transmute.gaining
  val removing = transmute.removing
  if (gaining.className != removing.className) return null
  if (
      gaining.arguments != listOf(describers.ownerExpression) ||
          removing.arguments != listOf(describers.anyoneExpression) ||
          gaining.refinement != null ||
          removing.refinement != null ||
          gaining.complement ||
          removing.complement
  ) {
    return null
  }
  if (!describers.concrete(gaining.className)) return null
  if (describers.fact(gaining.className, ComponentDescriber::standardResource) != true) return null
  val count = (transmute.count as? ActualScalar)?.value ?: return null
  val noun = describers.componentNoun(gaining.className, count)
  return clause(
      "steal",
      NounPhrase.text("up to $count $noun"),
      Modifier.Phrase("from any player"),
  )
}

private fun renderCardResourceChange(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val change = instruction as? Instruction.Change ?: return null
  val expression = change.gaining ?: change.removing ?: return null
  if (expression.refinement != null || expression.complement) return null
  val count = (change.count as? ActualScalar)?.value ?: return null
  val noun = describers.cardResourceNounPhrase(expression.className, count) ?: return null
  if (instruction is Remove) {
    return when {
      expression.simple && (change.intensity == null || change.intensity == MANDATORY) ->
          clause("remove", noun, Modifier.Phrase("from any card"))
      expression.arguments == listOf(describers.anyoneExpression) && change.intensity == OPTIONAL ->
          clause(
              "remove",
              NounPhrase.text("up to $count ${noun.noun()}"),
              Modifier.Phrase("from any player"),
          )
      else -> null
    }
  }
  if (change.intensity != null && change.intensity != MANDATORY) return null
  val target =
      when {
        expression.arguments == listOf(describers.thisExpression) -> "this card"
        expression.arguments.size == 1 ->
            describers.renderCardResourceHolder(expression.arguments.single()) ?: return null
        expression.arguments.isNotEmpty() -> return null
        else -> "any card"
      }
  return clause("add", noun, Modifier.Phrase("to $target"))
}

private fun renderProductionChange(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val change = instruction as? Instruction.Change ?: return null
  if (change.intensity != null && change.intensity != MANDATORY) return null
  val gaining =
      when (change) {
        is Gain -> true
        is Remove -> false
        is Transmute -> return renderProductionConversion(change, describers)
      }
  val expression = change.gaining ?: change.removing ?: return null
  val (ownerArguments, resourceClassName) =
      describers.productionExpression(expression) ?: return null
  val owner =
      when {
        ownerArguments.isEmpty() -> "your"
        !gaining && ownerArguments == listOf(describers.anyoneExpression) -> "any player's"
        else -> return null
      }
  val count = (change.count as? ActualScalar)?.value ?: return null
  val steps = if (count == 1) "step" else "steps"
  val production =
      "$owner ${describers.componentNoun(resourceClassName, 1)} production $count $steps"
  return clause(if (gaining) "increase" else "decrease", NounPhrase.text(production))
}

private fun renderProductionConversion(
    transmute: Transmute,
    describers: Describers,
): Clause? {
  val scalar = transmute.count as? XScalar ?: return null
  if (scalar.multiple != 1) return null
  val (gainingOwners, gainingResource) =
      describers.productionExpression(transmute.gaining) ?: return null
  val (removingOwners, removingResource) =
      describers.productionExpression(transmute.removing) ?: return null
  if (
      gainingOwners.isNotEmpty() ||
          removingOwners.isNotEmpty() ||
          gainingResource == removingResource
  ) {
    return null
  }
  val decrease =
      clause(
          "decrease",
          NounPhrase.text(
              "your ${describers.componentNoun(removingResource, 1)} production one or more steps"
          ),
      )
  val increase =
      clause(
          "increase",
          NounPhrase.text(
              "your ${describers.componentNoun(gainingResource, 1)} production the same number of steps"
          ),
      )
  return Clause.Coordinated(Coordination(listOf(decrease, increase), Conjunction.AND))
}

private fun renderTrackChange(
    instruction: Instruction,
    description: ComponentDescriber.Track,
): Clause? {
  val gain = concreteMandatoryGain(instruction)
  val removal = concreteMandatoryRemoval(instruction)
  val (_, count) = gain ?: removal ?: return null
  val steps = if (count == 1) "step" else "steps"
  return clause(
      if (gain != null) "raise" else "lower",
      NounPhrase.text("${description.subject} $count $steps"),
  )
}

private fun concreteMandatoryGain(instruction: Instruction): Pair<ClassName, Int>? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity != null && gain.intensity != MANDATORY) return null
  if (!gain.gaining.simple) return null
  val count = (gain.count as? ActualScalar)?.value ?: return null
  return gain.gaining.className to count
}

private fun concreteMandatoryRemoval(instruction: Instruction): Pair<ClassName, Int>? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity != null && removal.intensity != MANDATORY) return null
  if (!removal.removing.simple) return null
  val count = (removal.count as? ActualScalar)?.value ?: return null
  return removal.removing.className to count
}

private fun Describers.renderCardResourceHolder(expression: Expression): String? {
  return renderCardResourceHolder(expression, owned = false)
}

private fun Describers.renderOwnedCardResourceHolder(expression: Expression): String? {
  return renderCardResourceHolder(expression, owned = true)
}

private fun Describers.renderCardResourceHolder(
    expression: Expression,
    owned: Boolean,
): String? {
  if (expression.arguments.isNotEmpty() || expression.complement) return null
  val holder = fact(expression.className, ComponentDescriber::cardResourceHolder) ?: return null
  val refinement = expression.refinement ?: return null
  if (refinement.forgiving) return null
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  if (minimum.target != 1) return null
  val metric = minimum.metric as? Metric.Count ?: return null
  if (!metric.expression.simple) return null
  val (tag) = tagName(metric.expression.className) ?: return null
  return if (owned) "one of your $tag ${holder.plural}"
  else "${indefiniteArticle(tag)} $tag ${holder.singular}"
}

private fun clause(verb: String, noun: NounPhrase, vararg modifiers: Modifier): Clause.Simple =
    Clause.Simple(Predicate(verb, Coordination.one(noun), modifiers.toList()))
