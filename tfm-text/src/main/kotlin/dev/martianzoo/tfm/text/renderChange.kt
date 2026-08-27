package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.Dependency.Key

/** Interprets one Pets state change from passive component construction facts. */
internal fun renderChange(
    instruction: Instruction,
    describers: Describers,
): Rendering<Clause?> {
  val expression =
      when (instruction) {
        is Gain -> instruction.gaining
        is Remove -> instruction.removing
        is Transmute -> instruction.gaining
        else -> return Rendering.unresolved(instruction, RefusalReason.UNKNOWN_CHANGE_FRAME, null)
      }
  val clause = renderChangeOrNull(instruction, expression, describers)
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
): Clause? {
  if (instruction is Transmute) {
    renderCardResourceDrawExchange(instruction, describers)?.let {
      return it
    }
  }
  if (describers.isProduction(expression.className))
      return renderProductionChange(instruction, describers)
  describers.changeFrame(expression.className)?.let { frame ->
    return when (frame) {
      ComponentDescriber.ChangeFrame.Countable -> renderCountableChange(instruction, describers)
      is ComponentDescriber.ChangeFrame.Held -> renderCardResourceChange(instruction, describers)
      is ComponentDescriber.ChangeFrame.Scale -> renderScaleChange(instruction, frame)
      is ComponentDescriber.ChangeFrame.Positioned ->
          renderPlacement(instruction, frame, describers)
      ComponentDescriber.ChangeFrame.Deck ->
          renderDiscard(instruction, describers) ?: renderDraw(instruction, describers)
      is ComponentDescriber.ChangeFrame.Procedure -> renderProcedure(instruction, frame)
      is ComponentDescriber.ChangeFrame.Wrapper -> renderWrapper(instruction, frame, describers)
      ComponentDescriber.ChangeFrame.Play -> renderCardPlay(instruction, describers)
    }
  }
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
  if (describers.isProduction(expression.className)) {
    return RefusalReason.UNSUPPORTED_PRODUCTION_CHANGE
  }
  return when (describers.changeFrame(expression.className)) {
    ComponentDescriber.ChangeFrame.Deck ->
        if (instruction is Gain) RefusalReason.UNSUPPORTED_DRAW
        else RefusalReason.UNSUPPORTED_DISCARD
    is ComponentDescriber.ChangeFrame.Held -> RefusalReason.UNSUPPORTED_CARD_RESOURCE_CHANGE
    is ComponentDescriber.ChangeFrame.Scale -> RefusalReason.UNSUPPORTED_TRACK_CHANGE
    is ComponentDescriber.ChangeFrame.Positioned -> RefusalReason.UNSUPPORTED_PLACEMENT_CHANGE
    ComponentDescriber.ChangeFrame.Countable -> RefusalReason.UNSUPPORTED_STANDARD_RESOURCE_CHANGE
    is ComponentDescriber.ChangeFrame.Procedure,
    is ComponentDescriber.ChangeFrame.Wrapper,
    ComponentDescriber.ChangeFrame.Play -> RefusalReason.UNSUPPORTED_DECLARED_CHANGE
    null -> RefusalReason.UNKNOWN_CHANGE_FRAME
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
  return clause(
      "discard",
      describers.quantifiedComponentNounPhrase(removal.removing.className, count),
  )
}

private fun renderDraw(
    instruction: Instruction,
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
  return clause("draw", describers.quantifiedComponentNounPhrase(gain.gaining.className, count))
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

private fun renderProcedure(
    instruction: Instruction,
    frame: ComponentDescriber.ChangeFrame.Procedure,
): Clause.Simple? {
  val gain = instruction as? Gain ?: return null
  if (
      gain.intensity.modality() != Modality.REQUIRED ||
          !gain.gaining.simple ||
          gain.count.fixedQuantity() != 1
  ) {
    return null
  }
  return frame.objectPhrase?.let { clause(frame.verb, NounPhrase.text(it)) }
      ?: Clause.Simple(Predicate(frame.verb))
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

private fun renderWrapper(
    instruction: Instruction,
    frame: ComponentDescriber.ChangeFrame.Wrapper,
    describers: Describers,
): Clause? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  if (count != 1) return null
  val declaration = wrapperSubclassDeclaration(className, describers) ?: return null
  val effect = declaration.authoredEffectsWithActions.singleOrNull() ?: return null
  if (effect.automatic) return null
  val trigger = (effect.trigger as? OnGainOf)?.expression ?: return null
  val actionKey = Key(ClassName.cn("UseAction"), 0)
  val whichActionKey = Key(ClassName.cn("UseAction"), 1)
  val resolvedTrigger = describers.resolveExpression(trigger, actionKey) ?: return null
  if (
      !resolvedTrigger.hasOnlySourceDependencies(
          mapOf(
              actionKey to describers.thisExpression,
              whichActionKey to ClassName.cn("First").expression,
          )
      ) || trigger.refinement != null || trigger.complement
  ) {
    return null
  }
  val result =
      renderInstructions(effect.instruction, describers).clauses.singleOrNull() ?: return null
  return Clause.Prefaced(frame.preface, result)
}

private fun wrapperSubclassDeclaration(
    className: ClassName,
    describers: Describers,
): ClassDeclaration? {
  val componentClass = describers.expressions.classesByName.getValue(className)
  if (componentClass.abstract) return null
  val superclass = componentClass.directSuperclasses.singleOrNull() ?: return null
  if (describers.changeFrame(superclass.className) !is ComponentDescriber.ChangeFrame.Wrapper) {
    return null
  }
  val declaration = describers.declaration(className)
  val supertype = declaration.supertypes.singleOrNull()
  if (
      declaration.kind != ClassDeclaration.ClassKind.CONCRETE ||
          declaration.custom ||
          declaration.dependencies.isNotEmpty() ||
          supertype?.simple != true ||
          supertype.className != superclass.className ||
          declaration.invariants.isNotEmpty() ||
          declaration.defaultsDeclaration != ClassDeclaration.DefaultsDeclaration() ||
          declaration.properties.isNotEmpty()
  ) {
    return null
  }
  return declaration
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
  val resolved = describers.resolveCardResource(removing) ?: return null
  if (
      !describers.cardResourceHasHolder(resolved, describers.thisExpression) ||
          removing.refinement != null ||
          removing.complement
  ) {
    return null
  }
  val resource = describers.cardResourceNounPhrase(removing.className, count) ?: return null
  if (describers.changeFrame(gaining.className) !is ComponentDescriber.ChangeFrame.Deck) return null
  val drawPhrase = describers.quantifiedComponentNounPhrase(gaining.className, count).linearize()
  return clause(
      "remove",
      resource,
      Modifier.Phrase("from this card"),
      Modifier.Phrase("to draw $drawPhrase"),
  )
}

private fun renderCountableChange(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  concreteMandatoryGain(instruction)?.let { (className, count) ->
    val noun = describers.componentNounPhrase(className, count)
    return clause(
        "gain",
        if (describers.concrete(className)) noun
        else noun.copy(count = null, determiner = describers.indefiniteArticle(noun.noun())),
    )
  }
  (instruction as? Transmute)?.let {
    return renderStandardResourceTransfer(it, describers)
  }
  val removal = instruction as? Remove ?: return null
  val expression = removal.removing
  if (expression.refinement != null || expression.complement) return null
  val count = removal.count.fixedQuantity() ?: return null
  if (!describers.concrete(expression.className)) return null
  if (expression.simple && removal.intensity.modality() == Modality.REQUIRED) {
    return clause("remove", describers.componentNounPhrase(expression.className, count))
  }
  val resolved = describers.resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val player =
      resolved
          .sourceDependency(ownerKey)
          ?.takeIf { resolved.sourceDependencies.size == 1 }
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
  if (
      tagExpression.refinement != null ||
          tagExpression.complement ||
          resolveExpression(tagExpression)?.let { tag ->
            tag.sourceDependencies.isNotEmpty() &&
                !tag.hasOnlySourceDependency(Key(OWNED, 0), anyoneExpression)
          } != false
  ) {
    return null
  }
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
  val resolved = describers.resolveCardResource(expression) ?: return null
  val holder = describers.cardResourceHolder(resolved)
  if (instruction is Remove) {
    return when {
      resolved.sourceDependencies.isEmpty() && change.intensity.modality() == Modality.REQUIRED ->
          clause("remove", noun, Modifier.Phrase("from any card"))
      resolved.hasOnlySourceDependency(Key(OWNED, 0), describers.anyoneExpression) &&
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
          describers.cardResourceHasHolder(resolved, describers.thisExpression)
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
        describers.cardResourceHasHolder(resolved, describers.thisExpression) -> "this card"
        holder != null && describers.cardResourceHasHolder(resolved, holder) ->
            describers.renderCardResourceHolder(holder) ?: return null
        resolved.sourceDependencies.isNotEmpty() -> return null
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
  val production = productionExpression(expression, describers) ?: return null
  val owner =
      when {
        production.owner == null -> "your"
        !gaining && production.owner == describers.anyoneExpression -> "any player's"
        else -> return null
      }
  val count = change.count.fixedQuantity() ?: return null
  val steps = if (count == 1) "step" else "steps"
  val productionPhrase =
      "$owner ${describers.componentNoun(production.resource, 1)} production $count $steps"
  return clause(if (gaining) "increase" else "decrease", NounPhrase.text(productionPhrase))
}

private fun renderSelectedProductionChange(
    change: Instruction.Change,
    gaining: Boolean,
    expression: Expression,
    describers: Describers,
): Clause.Simple? {
  if (expression.refinement != null || expression.complement) {
    return null
  }
  val resource = selectedProductionResource(expression, describers) ?: return null
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
  val gaining = productionExpression(transmute.gaining, describers) ?: return null
  val removing = productionExpression(transmute.removing, describers) ?: return null
  if (gaining.owner != null || removing.owner != null || gaining.resource == removing.resource) {
    return null
  }
  val decrease =
      clause(
          "decrease",
          NounPhrase.text(
              "your ${describers.componentNoun(removing.resource, 1)} production one or more steps"
          ),
      )
  val increase =
      clause(
          "increase",
          NounPhrase.text(
              "your ${describers.componentNoun(gaining.resource, 1)} production the same number of steps"
          ),
      )
  return Clause.Coordinated(Coordination(listOf(decrease, increase), Conjunction.AND))
}

private fun renderScaleChange(
    instruction: Instruction,
    frame: ComponentDescriber.ChangeFrame.Scale,
): Clause? {
  if (instruction is Transmute) {
    if (
        instruction.intensity.modality() != Modality.REQUIRED ||
            !instruction.gaining.simple ||
            instruction.removing != instruction.gaining
    ) {
      return null
    }
    val count = instruction.count.fixedQuantity() ?: return null
    val steps = if (count == 1) "step" else "steps"
    val increase = clause("increase", NounPhrase.text("one ${frame.subject} $count $steps"))
    val decrease = clause("decrease", NounPhrase.text("another ${frame.subject} $count $steps"))
    return Clause.Coordinated(Coordination(listOf(increase, decrease), Conjunction.AND))
  }
  val gain = concreteMandatoryGain(instruction)
  val removal = concreteMandatoryRemoval(instruction)
  val (_, count) = gain ?: removal ?: return null
  val steps = if (count == 1) "step" else "steps"
  return clause(
      if (gain != null) "raise" else "lower",
      NounPhrase.text("${frame.subject} $count $steps"),
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
  val resolved = resolveExpression(expression) ?: return null
  if (resolved.sourceDependencies.isNotEmpty() || expression.complement) return null
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
