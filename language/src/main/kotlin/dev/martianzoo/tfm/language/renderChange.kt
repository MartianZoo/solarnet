package dev.martianzoo.tfm.language

import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.Dependency.Key

/** Interprets one Pets state change from passive component construction facts. */
internal fun renderChange(
    instruction: Instruction,
    describers: Describers,
    drawFilter: EnglishDrawFilter? = null,
): Rendering<Clause?> {
  val expression =
      when (instruction) {
        is Gain -> instruction.gaining
        is Remove -> instruction.removing
        is Transmute -> instruction.gaining
        else -> return Rendering.unresolved(instruction, RefusalReason.UNKNOWN_CHANGE_FRAME, null)
      }
  val clause = renderChangeOrNull(instruction, expression, describers, drawFilter)
  return if (clause != null) Rendering.resolved(clause)
  else
      Rendering.unresolved(
          instruction,
          changeRefusalReason(instruction, expression, describers),
          null,
      )
}

private fun renderChangeOrNull(
    instruction: Instruction,
    expression: Expression,
    describers: Describers,
    drawFilter: EnglishDrawFilter?,
): Clause? {
  describers.fact(expression.className, ComponentDescriber::directChange)?.let {
    renderDirectChange(instruction, it, describers, drawFilter)?.let { clause ->
      return clause
    }
  }
  if (describers.fact(expression.className, ComponentDescriber::discardable) == true) {
    renderDiscard(instruction, describers)?.let {
      return it
    }
  }
  if (instruction is Transmute) {
    renderCardResourceDrawExchange(instruction, describers)?.let {
      return it
    }
  }
  if (describers.fact(expression.className, ComponentDescriber::draw) == true)
      return renderDraw(instruction, drawFilter, describers)
  if (describers.isCardResource(expression.className))
      return renderCardResourceChange(instruction, describers)
  if (describers.isProduction(expression.className))
      return renderProductionChange(instruction, describers)
  describers.fact(expression.className, ComponentDescriber::track)?.let {
    return renderTrackChange(instruction, it)
  }
  describers.fact(expression.className, ComponentDescriber::placement)?.let {
    return renderPlacement(instruction, it, describers)
  }
  if (describers.isStandardResource(expression.className))
      return renderStandardResourceChange(instruction, describers)
  return null
}

private fun changeRefusalReason(
    instruction: Instruction,
    expression: Expression,
    describers: Describers,
): RefusalReason {
  if (expression.refinement != null || expression.complement) {
    return RefusalReason.REFINED_CHANGE_EXPRESSION
  }
  if (instruction is Instruction.Change && instruction.count.fixedQuantity() == null) {
    return RefusalReason.UNSUPPORTED_CHANGE_QUANTITY
  }
  return when {
    describers.fact(expression.className, ComponentDescriber::directChange) != null ->
        RefusalReason.UNSUPPORTED_DECLARED_CHANGE
    describers.fact(expression.className, ComponentDescriber::discardable) == true ->
        RefusalReason.UNSUPPORTED_DISCARD
    describers.fact(expression.className, ComponentDescriber::draw) == true ->
        RefusalReason.UNSUPPORTED_DRAW
    describers.isCardResource(expression.className) ->
        RefusalReason.UNSUPPORTED_CARD_RESOURCE_CHANGE
    describers.isProduction(expression.className) -> RefusalReason.UNSUPPORTED_PRODUCTION_CHANGE
    describers.fact(expression.className, ComponentDescriber::track) != null ->
        RefusalReason.UNSUPPORTED_TRACK_CHANGE
    describers.fact(expression.className, ComponentDescriber::placement) != null ->
        RefusalReason.UNSUPPORTED_PLACEMENT_CHANGE
    describers.isStandardResource(expression.className) ->
        RefusalReason.UNSUPPORTED_STANDARD_RESOURCE_CHANGE
    else -> RefusalReason.UNKNOWN_CHANGE_FRAME
  }
}

private fun renderDiscard(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity.modality() != Modality.REQUIRED) return null
  if (!removal.removing.simple) return null
  val count = removal.count.fixedQuantity() ?: return null
  val discarded = describers.componentNounPhrase(removal.removing.className, count)
  return clause(
      "discard",
      if (count == 1) discarded.copy(count = null, determiner = "a") else discarded,
  )
}

private fun renderDraw(
    instruction: Instruction,
    filter: EnglishDrawFilter?,
    describers: Describers,
): Clause.Simple? {
  val gain = instruction as? Gain ?: return null
  if (
      gain.intensity.modality() != Modality.REQUIRED ||
          !gain.gaining.simple ||
          !describers.concrete(gain.gaining.className)
  ) {
    return null
  }
  val count = gain.count.fixedQuantity() ?: return null
  if (filter == null) {
    val noun = describers.componentNoun(gain.gaining.className, count)
    val amount = if (count == 1) "${describers.indefiniteArticle(noun)} $noun" else "$count $noun"
    return clause("draw", NounPhrase.text(amount))
  }
  val cards =
      when (filter) {
        is EnglishDrawFilter.Tag -> {
          val (tag) = describers.tagName(filter.className) ?: return null
          NounPhrase.text(if (count == 1) "a $tag card" else "$count $tag cards")
        }
        is EnglishDrawFilter.Icon -> {
          val resource = describers.cardResourceNoun(filter.className, 1) ?: return null
          val text =
              if (count == 1) {
                "a card with ${describers.indefiniteArticle(resource)} $resource icon"
              } else {
                "$count cards with $resource icons"
              }
          NounPhrase.text(text)
        }
        EnglishDrawFilter.Requirements ->
            NounPhrase.text(
                if (count == 1) "a card with a requirement" else "$count cards with requirements"
            )
      }
  return clause("draw", cards)
}

internal fun isProductionChange(instruction: Instruction, describers: Describers): Boolean {
  val expression =
      (instruction as? Instruction.Change)?.let { it.gaining ?: it.removing } ?: return false
  return describers.isProduction(expression.className)
}

internal fun isCoalescibleStandardResourceGain(
    instruction: Instruction,
    describers: Describers,
): Boolean {
  val expression = (instruction as? Gain)?.gaining ?: return false
  return describers.isStandardResource(expression.className)
}

internal fun standardResourceGain(
    instruction: Instruction,
    describers: Describers,
): Pair<ClassName, Int>? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  return (className to count).takeIf {
    describers.concrete(className) && describers.isStandardResource(className)
  }
}

private fun renderDirectChange(
    instruction: Instruction,
    description: ComponentDescriber.DirectChange,
    describers: Describers,
    drawFilter: EnglishDrawFilter?,
): Clause? {
  return when (description) {
    is ComponentDescriber.DirectChange.Gain -> {
      val (_, count) = concreteMandatoryGain(instruction) ?: return null
      if (count != description.count) return null
      clause("gain", NounPhrase(description.noun, count = count))
    }
    is ComponentDescriber.DirectChange.GainChoice -> {
      val gain = instruction as? Gain ?: return null
      if (gain.intensity.modality() != Modality.REQUIRED) return null
      if (!gain.gaining.simple || describers.concrete(gain.gaining.className)) return null
      if (gain.count.fixedQuantity() != 1) return null
      clause("gain", NounPhrase.text(description.objectPhrase))
    }
    is ComponentDescriber.DirectChange.Imperative -> {
      val gain = instruction as? Gain ?: return null
      if (gain.intensity.modality() != Modality.REQUIRED) return null
      if (!gain.gaining.simple) return null
      if (gain.count.fixedQuantity() != 1) return null
      clause(description.verb, NounPhrase.text(description.objectPhrase))
    }
    is ComponentDescriber.DirectChange.TrackTransfer ->
        renderTrackTransfer(instruction, description)
    is ComponentDescriber.DirectChange.Operation -> renderOperation(instruction, description)
    ComponentDescriber.DirectChange.PlayCard -> renderCardPlay(instruction, describers)
    ComponentDescriber.DirectChange.NextPlayedCardAdjustment ->
        renderNextPlayedCardAdjustment(instruction, describers)
    ComponentDescriber.DirectChange.ProductionBoxCopy ->
        renderProductionBoxCopy(instruction, describers)
    ComponentDescriber.DirectChange.FirstAction ->
        renderFirstAction(instruction, describers, drawFilter)
    ComponentDescriber.DirectChange.TopCardPurchase ->
        renderTopCardPurchase(instruction, describers)
  }
}

private fun renderCardPlay(instruction: Instruction, describers: Describers): Clause.Simple? {
  val gain = instruction as? Gain ?: return null
  if (
      gain.intensity.modality() != Modality.REQUIRED ||
          gain.gaining.refinement != null ||
          gain.gaining.complement ||
          gain.count.fixedQuantity() != 1
  ) {
    return null
  }
  val card = describers.representedClass(gain.gaining) ?: return null
  val noun = describers.componentNoun(card.className, 1)
  return clause("play", NounPhrase.text("${describers.indefiniteArticle(noun)} $noun"))
}

private fun renderOperation(
    instruction: Instruction,
    description: ComponentDescriber.DirectChange.Operation,
): Clause.Simple? {
  val gain = instruction as? Gain ?: return null
  if (
      gain.intensity.modality() != Modality.REQUIRED ||
          gain.gaining.refinement != null ||
          gain.gaining.complement ||
          gain.count.fixedQuantity() != 1
  ) {
    return null
  }
  return Clause.Simple(Predicate(description.verb))
}

private fun renderTrackTransfer(
    instruction: Instruction,
    description: ComponentDescriber.DirectChange.TrackTransfer,
): Clause? {
  val transmute = instruction as? Transmute ?: return null
  if (
      transmute.intensity.modality() != Modality.REQUIRED ||
          !transmute.gaining.simple ||
          transmute.removing != transmute.gaining
  ) {
    return null
  }
  val count = transmute.count.fixedQuantity() ?: return null
  val steps = if (count == 1) "step" else "steps"
  val increase = clause("increase", NounPhrase.text("one ${description.trackNoun} $count $steps"))
  val decrease =
      clause("decrease", NounPhrase.text("another ${description.trackNoun} $count $steps"))
  return Clause.Coordinated(Coordination(listOf(increase, decrease), Conjunction.AND))
}

private fun renderFirstAction(
    instruction: Instruction,
    describers: Describers,
    drawFilter: EnglishDrawFilter?,
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
      renderInstructions(effect.instruction, describers, drawFilter).clauses.singleOrNull()
          ?: return null
  return Clause.Prefaced("as your first action", result)
}

private fun renderProductionBoxCopy(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity.modality() != Modality.REQUIRED) return null
  if (
      !describers.concrete(gain.gaining.className) ||
          gain.gaining.refinement != null ||
          gain.gaining.complement ||
          gain.count.fixedQuantity() != 1
  ) {
    return null
  }
  val card = gain.gaining.arguments.singleOrNull() ?: return null
  val holder = describers.renderOwnedCardResourceHolder(card) ?: return null
  return clause("duplicate", NounPhrase.text("the production box of $holder"))
}

private fun renderNextPlayedCardAdjustment(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  if (count != 1) return null
  val declaration = describers.directChangeSubclassDeclaration(className) ?: return null
  val lifecycle =
      declaration.effects.singleOrNull { effect ->
        val removal = effect.instruction as? Remove ?: return@singleOrNull false
        removal.intensity.modality() == Modality.REQUIRED &&
            removal.removing == describers.thisExpression &&
            removal.count.fixedQuantity() == 1
      } ?: return null
  val effect = declaration.effects.singleOrNull { it !== lifecycle } ?: return null
  if (lifecycle.trigger != effect.trigger || lifecycle.automatic != effect.automatic) return null
  val played = (effect.trigger as? OnGainOf)?.expression ?: return null
  if (
      !played.simple ||
          describers.fact(played.className, ComponentDescriber::playTrigger) !=
              ComponentDescriber.PlayTrigger.CARD
  ) {
    return null
  }
  owedReduction(effect.instruction, describers)?.let { reduction ->
    if (!effect.automatic) return null
    return Clause.Simple(
        predicate =
            Predicate(
                "costs",
                Coordination.one(NounPhrase.text("${reduction.count} ${reduction.noun} less")),
            ),
        subject = NounPhrase.text("the next card you play this generation"),
    )
  }
  if (effect.automatic) return null
  val requirement = effect.instruction as? Remove ?: return null
  if (
      requirement.intensity.modality() != Modality.BEST_EFFORT ||
          requirement.removing.refinement != null ||
          requirement.removing.complement ||
          describers.fact(
              requirement.removing.className,
              ComponentDescriber::requirementShortfall,
          ) != true
  ) {
    return null
  }
  val adjustment = requirement.count.fixedQuantity() ?: return null
  val target = describers.representedClass(requirement.removing) ?: return null
  val kind = describers.fact(target.className, ComponentDescriber::requirementKind) ?: return null
  val steps = if (adjustment == 1) "step" else "steps"
  return Clause.Simple(
      predicate =
          Predicate(
              "may treat",
              Coordination.one(
                  NounPhrase.text("the $kind requirement of the next card you play this generation")
              ),
              listOf(Modifier.Phrase("as if it is $adjustment $steps lower or higher")),
          ),
      subject = NounPhrase.text("you"),
  )
}

private fun renderTopCardPurchase(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val gain = instruction as? Gain ?: return null
  if (
      gain.intensity.modality() != Modality.OPTIONAL ||
          !gain.gaining.simple ||
          !describers.concrete(gain.gaining.className) ||
          gain.count.fixedQuantity() != 1
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
  if (transmute.intensity.modality() != Modality.REQUIRED) return null
  val count = transmute.count.fixedQuantity() ?: return null
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
  val drawPhrase =
      if (count == 1) cards.copy(count = null, determiner = "a").linearize() else cards.linearize()
  return clause(
      "remove",
      resource,
      Modifier.Phrase("from this card"),
      Modifier.Phrase("to draw $drawPhrase"),
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
  val count = removal.count.fixedQuantity() ?: return null
  if (!describers.concrete(expression.className)) return null
  if (!describers.isStandardResource(expression.className)) return null
  if (expression.simple && removal.intensity.modality() == Modality.REQUIRED) {
    return clause("remove", describers.componentNounPhrase(expression.className, count))
  }
  val resolved = describers.resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val player =
      resolved
          .sourceDependency(ownerKey)
          ?.takeIf {
            resolved.sourceDependencies.size == 1
          }
          ?.let { describers.renderEligiblePlayer(it) }
  if (player != null && removal.intensity.modality() == Modality.OPTIONAL) {
    val noun = describers.componentNoun(expression.className, count)
    return clause(
        "remove",
        NounPhrase.text("up to $count $noun"),
        Modifier.Phrase("from $player"),
    )
  }
  return null
}

private fun Describers.renderEligiblePlayer(expression: Expression): String? {
  if (expression == anyoneExpression) return "any player"
  if (
      expression.className != anyoneExpression.className ||
          resolveExpression(expression)?.sourceDependencies?.isNotEmpty() != false ||
          expression.complement
  ) {
    return null
  }
  val refinement = expression.refinement ?: return null
  if (refinement.forgiving) return null
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  if (minimum.target != 1) return null
  val tagExpression = (minimum.metric as? Metric.Count)?.expression ?: return null
  if (!tagExpression.simple) return null
  val tag = tagName(tagExpression.className)?.first ?: return null
  return "a player with ${indefiniteArticle(tag)} $tag tag"
}

private fun renderStandardResourceTransfer(
    transmute: Transmute,
    describers: Describers,
): Clause? {
  val gaining = transmute.gaining
  val removing = transmute.removing
  if (gaining.className != removing.className) return null
  if (
      gaining.refinement != null ||
          removing.refinement != null ||
          gaining.complement ||
          removing.complement
  ) {
    return null
  }
  if (!describers.concrete(gaining.className)) return null
  if (!describers.isStandardResource(gaining.className)) return null
  val recipient = renderTransferParty(gaining, describers) ?: return null
  val payer = renderTransferParty(removing, describers) ?: return null
  val (verb, preposition, otherParty) =
      when {
        recipient == TransferParty.YOU && payer != TransferParty.YOU ->
            Triple("steal", "from", payer)
        payer == TransferParty.YOU && recipient != TransferParty.YOU ->
            Triple("pay", "to", recipient)
        else -> return null
      }
  val count = transmute.count.fixedQuantity() ?: return null
  val noun = describers.componentNoun(gaining.className, count)
  val amount =
      if (transmute.intensity.modality() == Modality.OPTIONAL) "up to $count $noun"
      else "$count $noun"
  val completion =
      if (transmute.intensity.modality() == Modality.BEST_EFFORT)
          Modifier.Supplement("or as much as possible")
      else null
  return clause(
      verb,
      NounPhrase.text(amount),
      Modifier.Phrase("$preposition ${otherParty.objectPhrase}"),
      *listOfNotNull(completion).toTypedArray(),
  )
}

private enum class TransferParty(val objectPhrase: String) {
  YOU("you"),
  ANY_PLAYER("any player"),
  THAT_PLAYER("that player"),
}

private fun renderTransferParty(
    expression: Expression,
    describers: Describers,
): TransferParty? {
  val resolved = describers.resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  return when {
    resolved.sourceDependencies.isEmpty() -> TransferParty.YOU
    resolved.hasOnlySourceDependency(ownerKey, describers.ownerExpression) -> TransferParty.YOU
    resolved.hasOnlySourceDependency(ownerKey, describers.anyoneExpression) ->
        TransferParty.ANY_PLAYER
    resolved.hasOnlySourceDependency(ownerKey, describers.playerExpression) ->
        TransferParty.THAT_PLAYER
    else -> null
  }
}

private fun renderCardResourceChange(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val change = instruction as? Instruction.Change ?: return null
  val expression = change.gaining ?: change.removing ?: return null
  if (expression.refinement != null || expression.complement) return null
  val count = change.count.fixedQuantity() ?: return null
  val noun = describers.cardResourceNounPhrase(expression.className, count) ?: return null
  if (instruction is Remove) {
    return when {
      expression.simple && change.intensity.modality() == Modality.REQUIRED ->
          clause("remove", noun, Modifier.Phrase("from any card"))
      expression.arguments == listOf(describers.anyoneExpression) &&
          change.intensity.modality() == Modality.OPTIONAL ->
          clause(
              "remove",
              NounPhrase.text("up to $count ${noun.noun()}"),
              Modifier.Phrase("from any player"),
          )
      else -> null
    }
  }
  if (
      change.intensity.modality() == Modality.OPTIONAL &&
          expression.arguments == listOf(describers.thisExpression)
  ) {
    return clause(
        "add",
        NounPhrase.text("up to $count ${noun.noun()}"),
        Modifier.Phrase("to this card"),
    )
  }
  if (change.intensity.modality() != Modality.REQUIRED) return null
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
  if (change.intensity.modality() != Modality.REQUIRED) return null
  val gaining =
      when (change) {
        is Gain -> true
        is Remove -> false
        is Transmute -> return renderProductionConversion(change, describers)
      }
  val expression = change.gaining ?: change.removing ?: return null
  renderSelectedProductionChange(change, gaining, expression, describers)?.let {
    return it
  }
  val (ownerArguments, resourceClassName) =
      describers.productionExpression(expression) ?: return null
  val owner =
      when {
        ownerArguments.isEmpty() -> "your"
        !gaining && ownerArguments == listOf(describers.anyoneExpression) -> "any player's"
        else -> return null
      }
  val count = change.count.fixedQuantity() ?: return null
  val steps = if (count == 1) "step" else "steps"
  val production =
      "$owner ${describers.componentNoun(resourceClassName, 1)} production $count $steps"
  return clause(if (gaining) "increase" else "decrease", NounPhrase.text(production))
}

private fun renderSelectedProductionChange(
    change: Instruction.Change,
    gaining: Boolean,
    expression: Expression,
    describers: Describers,
): Clause.Simple? {
  if (
      !describers.isProduction(expression.className) ||
          expression.refinement != null ||
          expression.complement ||
          expression.arguments.size != 1
  ) {
    return null
  }
  val resource = describers.representedClassArgument(expression.arguments.single()) ?: return null
  if (
      !describers.isStandardResource(resource.className) ||
          describers.concrete(resource.className) ||
          resource.complement
  ) {
    return null
  }
  val refinement = resource.refinement?.takeIf { !it.forgiving } ?: return null
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  if (minimum.target != 1) return null
  val selector = (minimum.metric as? Metric.Count)?.expression ?: return null
  if (!selector.simple) return null
  val phrase =
      describers.fact(selector.className, ComponentDescriber::productionSelection) ?: return null
  val count = change.count.fixedQuantity() ?: return null
  val steps = if (count == 1) "step" else "steps"
  return clause(
      if (gaining) "increase" else "decrease",
      NounPhrase.text("$phrase $count $steps"),
  )
}

private fun renderProductionConversion(
    transmute: Transmute,
    describers: Describers,
): Clause? {
  val scalar = transmute.count.variableQuantity() ?: return null
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
  if (gain.intensity.modality() != Modality.REQUIRED) return null
  if (!gain.gaining.simple) return null
  val count = gain.count.fixedQuantity() ?: return null
  return gain.gaining.className to count
}

private fun concreteMandatoryRemoval(instruction: Instruction): Pair<ClassName, Int>? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity.modality() != Modality.REQUIRED) return null
  if (!removal.removing.simple) return null
  val count = removal.count.fixedQuantity() ?: return null
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
  val metric = minimum.metric as? Metric.Count ?: return null
  if (!metric.expression.simple) return null
  tagName(metric.expression.className)?.let { (tag) ->
    if (minimum.target != 1) return null
    return if (owned) "one of your $tag ${holder.plural}"
    else "${indefiniteArticle(tag)} $tag ${holder.singular}"
  }
  val resource =
      cardResourceNoun(metric.expression.className, maxOf(2, minimum.target)) ?: return null
  val subject = if (owned) "one of your ${holder.plural}" else "a ${holder.singular}"
  return "$subject with ${minimum.target} or more $resource on it"
}

private fun clause(verb: String, noun: NounPhrase, vararg modifiers: Modifier): Clause.Simple =
    Clause.Simple(Predicate(verb, Coordination.one(noun), modifiers.toList()))
