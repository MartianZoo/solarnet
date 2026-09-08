package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
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
    references: TypeVariableReferences = TypeVariableReferences.EMPTY,
): Rendering<Clause?> {
  val expression =
      when (instruction) {
        is Gain -> instruction.gaining
        is Remove -> instruction.removing
        is Transmute -> instruction.gaining
        else -> return Rendering.unresolved(instruction, RefusalReason.UNKNOWN_CHANGE_FRAME, null)
      }
  val clause = renderChangeOrNull(instruction, expression, describers, references)
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
    references: TypeVariableReferences,
): Clause? {
  if (instruction is Transmute) {
    renderPlayedEventRecovery(instruction, describers)?.let {
      return it
    }
    renderCardResourceDrawExchange(instruction, describers)?.let {
      return it
    }
    renderPositionedConversion(instruction, describers)?.let {
      return it
    }
  }
  renderActionUseChange(instruction, describers)?.let {
    return it
  }
  renderTypeVariableResourceChange(instruction, expression, describers, references)?.let {
    return it
  }
  if (describers.isProduction(expression.className))
      return renderProductionChange(instruction, describers)
  describers.changeFrame(expression.className)?.let { frame ->
    return when (frame) {
      ComponentDescriber.ChangeFrame.Countable -> renderCountableChange(instruction, describers)
      is ComponentDescriber.ChangeFrame.Held -> renderCardResourceChange(instruction, describers)
      is ComponentDescriber.ChangeFrame.Scale ->
          renderScaleChange(instruction, expression, frame, references)
      is ComponentDescriber.ChangeFrame.Positioned ->
          renderPlacement(instruction, frame, describers)
      ComponentDescriber.ChangeFrame.Deck ->
          renderDiscard(instruction, describers) ?: renderDraw(instruction, describers)
      is ComponentDescriber.ChangeFrame.Procedure -> renderProcedure(instruction, frame, describers)
      ComponentDescriber.ChangeFrame.RequiredAction -> renderRequiredAction(instruction, describers)
      ComponentDescriber.ChangeFrame.NextCardEffect -> renderNextCardEffect(instruction, describers)
      ComponentDescriber.ChangeFrame.Play -> renderCardPlay(instruction, describers)
    }
  }
  return null
}

private fun renderTypeVariableResourceChange(
    instruction: Instruction,
    expression: Expression,
    describers: Describers,
    references: TypeVariableReferences,
): Clause.Simple? {
  val change = instruction as? Instruction.Change ?: return null
  if (
      change.intensity.modality() != Modality.REQUIRED ||
          !expression.simple ||
          expression.refinement != null ||
          expression.complement
  ) {
    return null
  }
  val variable = references.variableUsedAt(expression) ?: return null
  if (!describers.isStandardResource(variable.bound.rootClass.className)) return null
  val count = change.count.fixedQuantity() ?: return null
  val antecedent = NounPhrase("resource", determiner = Determiner.THAT)
  val resource =
      if (count == 1) antecedent
      else NounPhrase.text("$count").withModifier(Modifier.Relation("of", antecedent))
  val verb =
      when (change) {
        is Gain -> "gain"
        is Remove -> "remove"
        is Transmute -> return null
      }
  return clause(verb, resource)
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
    ComponentDescriber.ChangeFrame.RequiredAction,
    ComponentDescriber.ChangeFrame.NextCardEffect,
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
    describers: Describers,
): Clause.Simple? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity.modality() != Modality.REQUIRED || gain.count.fixedQuantity() != 1) {
    return null
  }
  val objectPhrase = frame.objectPhrase ?: return Clause.Simple(Predicate(Verb(frame.verb)))
  val targetRelation = frame.cardTargetRelation
  if (targetRelation == null) {
    if (!gain.gaining.simple) return null
    return clause(frame.verb, NounPhrase.text(objectPhrase))
  }
  if (gain.gaining.refinement != null || gain.gaining.complement) return null
  val target = gain.gaining.arguments.singleOrNull()?.let(describers::cardSelector) ?: return null
  return clause(
      frame.verb,
      NounPhrase.text(objectPhrase).withModifier(Modifier.Relation(targetRelation, target)),
  )
}

private fun renderActionUseChange(
    instruction: Instruction,
    describers: Describers,
): Clause.Simple? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity.modality() != Modality.REQUIRED || gain.count.fixedQuantity() != 1) return null
  val action =
      describers.actionUseEvent(Effect.Trigger.OnGainOf.create(gain.gaining))?.provider
          ?: return null
  return describers.renderActionUse(action)?.let { clause("use", it) }
}

private fun renderPositionedConversion(
    transmute: Transmute,
    describers: Describers,
): Clause.Simple? {
  if (transmute.intensity.modality() != Modality.REQUIRED) return null
  if (transmute.count.fixedQuantity() != 1) return null
  val gaining = transmute.gaining
  val removing = transmute.removing
  if (
      gaining.refinement != null ||
          removing.refinement != null ||
          gaining.complement ||
          removing.complement ||
          !describers.concrete(gaining.className) ||
          !describers.concrete(removing.className)
  ) {
    return null
  }
  val gainingFrame = describers.positionedFrame(gaining.className) ?: return null
  val removingFrame = describers.positionedFrame(removing.className) ?: return null
  val gainingPlacement = resolvePlacementExpression(gaining, describers) ?: return null
  val removingPlacement = resolvePlacementExpression(removing, describers) ?: return null
  if (
      gainingPlacement.owner != null ||
          removingPlacement.owner != null ||
          gainingPlacement.unknownDependencies.isNotEmpty() ||
          removingPlacement.unknownDependencies.isNotEmpty()
  ) {
    return null
  }
  val site = gainingPlacement.sites.singleOrNull() ?: return null
  if (removingPlacement.sites.singleOrNull() != site || !site.simple) return null
  val siteDescription = describers.placementSite(site.className) ?: return null
  val siteNoun = describers.describedNoun(site.className, siteDescription.noun, 1)
  val source =
      NounPhrase(removingFrame.singular, determiner = removingFrame.determiner)
          .withModifier(Modifier.Relation("on", NounPhrase(siteNoun, determiner = Determiner.ANY)))
  val destination =
      NounPhrase(gainingFrame.singular, determiner = gainingFrame.determiner)
          .withModifier(Modifier.Relation("on", NounPhrase(siteNoun, determiner = Determiner.THAT)))
  return clause("change", source, Modifier.Relation("into", destination))
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
  return clause("play", NounPhrase(noun, determiner = Determiner.INDEFINITE))
}

private fun renderRequiredAction(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  if (count != 1) return null
  val declaration = behaviorSubclassDeclaration(className, describers) ?: return null
  val effect = declaration.authoredEffectsWithActions.singleOrNull() ?: return null
  if (effect.automatic) return null
  val actionUse = describers.actionUseEvent(effect.trigger) ?: return null
  if (actionUse.provider != describers.thisExpression) return null
  if (actionUse.slot != ClassName.cn("Action1").expression) return null
  val result =
      renderInstructions(effect.instruction, describers).clauses.singleOrNull() ?: return null
  return Clause.Prefaced(Clause.Preface.FirstAction, result)
}

private fun behaviorSubclassDeclaration(
    className: ClassName,
    describers: Describers,
): ClassDeclaration? {
  val componentClass = describers.expressions.classesByName.getValue(className)
  if (componentClass.abstract) return null
  val superclass = componentClass.directSuperclasses.singleOrNull() ?: return null
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

private fun renderNextCardEffect(
    instruction: Instruction,
    describers: Describers,
): Clause.Simple? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  if (count != 1) return null
  val declaration = behaviorSubclassDeclaration(className, describers) ?: return null
  if (declaration.authoredEffectsWithActions.size != 1) return null
  val effect =
      declaration.authoredEffects.singleOrNull()?.let(describers::prepareForRendering)
          ?: return null
  val nextCard = NounPhrase.text("the next card you play this generation")
  paymentDiscount(effect, describers)?.let { discount ->
    if (discount.categoryReduction || discount.reduction.count == 0) return null
    return Clause.Simple(
        subject = nextCard,
        predicate =
            Predicate(
                Verb("costs", "cost"),
                Coordination.one(discount.reduction.phrase.withModifier(Modifier.Phrase("less"))),
            ),
    )
  }
  return renderRequirementFlexibilityResult(effect, describers, nextCard)
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
        else noun.copy(count = null, determiner = Determiner.INDEFINITE),
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
    val amount = describers.componentNounPhrase(expression.className, count).atMost()
    return Clause.Simple(
        Predicate(
            Verb("may remove"),
            Coordination.one(amount),
            listOf(Modifier.Relation("from", player)),
        ),
        NounPhrase.you(),
    )
  }
  return null
}

private fun Describers.renderEligiblePlayer(expression: Expression): NounPhrase? {
  if (expression == anyoneExpression) return NounPhrase("player", determiner = Determiner.ANY)
  if (expression == playerExpression) return NounPhrase("player", determiner = Determiner.THAT)
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
  val tag = tagName(tagExpression.className) ?: return null
  return NounPhrase("player", determiner = Determiner.INDEFINITE)
      .withModifier(
          Modifier.Relation(
              "with",
              NounPhrase("$tag tag", determiner = Determiner.INDEFINITE),
          )
      )
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
  val noun = describers.componentNounPhrase(gaining.className, count)
  val amount = if (transmute.intensity.modality() == Modality.OPTIONAL) noun.atMost() else noun
  val completion =
      if (transmute.intensity.modality() == Modality.BEST_EFFORT)
          Modifier.Supplement("or as much as possible")
      else null
  val predicate =
      Predicate(
          Verb(if (transmute.intensity.modality() == Modality.OPTIONAL) "may $verb" else verb),
          Coordination.one(amount),
          listOfNotNull(
              Modifier.Phrase("$preposition ${otherParty.objectPhrase}"),
              completion,
          ),
      )
  return Clause.Simple(
      predicate,
      if (transmute.intensity.modality() == Modality.OPTIONAL) NounPhrase.you() else null,
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
  val resolved = describers.resolveHeldResource(expression) ?: return null
  val holder = describers.heldResourceHolder(resolved)
  if (instruction is Remove) {
    return when {
      resolved.sourceDependencies.isEmpty() && change.intensity.modality() == Modality.REQUIRED ->
          clause("remove", noun, Modifier.Phrase("from any card"))
      resolved.hasOnlySourceDependency(Key(OWNED, 0), describers.anyoneExpression) &&
          change.intensity.modality() == Modality.OPTIONAL ->
          Clause.Simple(
              Predicate(
                  Verb("may remove"),
                  Coordination.one(noun.atMost()),
                  listOf(
                      Modifier.Relation(
                          "from",
                          NounPhrase("player", determiner = Determiner.ANY),
                      )
                  ),
              ),
              NounPhrase.you(),
          )
      else -> null
    }
  }
  if (
      change.intensity.modality() == Modality.OPTIONAL &&
          describers.heldResourceHasHolder(resolved, describers.thisExpression)
  ) {
    return Clause.Simple(
        Predicate(
            Verb("may add"),
            Coordination.one(noun.atMost()),
            listOf(Modifier.Relation("to", NounPhrase("card", determiner = Determiner.THIS))),
        ),
        NounPhrase.you(),
    )
  }
  if (change.intensity.modality() == Modality.OPTIONAL) return null
  val target =
      when {
        describers.heldResourceHasHolder(resolved, describers.thisExpression) ->
            NounPhrase("card", determiner = Determiner.THIS)
        holder != null && describers.heldResourceHasHolder(resolved, holder) ->
            describers.renderCardResourceHolder(holder) ?: return null
        resolved.sourceDependencies.isNotEmpty() -> return null
        else -> NounPhrase("card", determiner = Determiner.ANY)
      }
  return clause("add", noun, Modifier.Relation("to", target))
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
  val first = refinement.requirement as? Requirement.Exact ?: return null
  if (first.target != 1 || !first.metric.isLowestStandardProductionRank(describers)) return null
  val count = change.count.fixedQuantity() ?: return null
  val steps = if (count == 1) "step" else "steps"
  return clause(
      if (gaining) "increase" else "decrease",
      NounPhrase.text("one of your lowest productions $count $steps"),
  )
}

private fun Metric.isLowestStandardProductionRank(describers: Describers): Boolean {
  val rank = this as? Metric.Rank ?: return false
  val selector = rank.selectorName
  if (
      selector.className != CLASS ||
          selector.arguments.singleOrNull()?.takeIf { it.simple }?.className != STANDARD_RESOURCE ||
          selector.complement
  ) {
    return false
  }
  val alternatives = (rank.metrics.singleOrNull() as? Metric.Or)?.metrics ?: return false
  if (alternatives.size != 2) return false
  val production =
      alternatives.singleOrNull { alternative ->
        val expression = alternative.expression
        describers.isProduction(expression.className) &&
            expression.arguments == listOf(selector.copy(complement = true)) &&
            expression.refinement == null &&
            !expression.complement
      } ?: return false
  val offset = alternatives.single { it != production }.expression
  return offset.arguments == listOf(selector) &&
      offset.refinement == null &&
      !offset.complement &&
      describers.fact(offset.className, ComponentDescriber::productionOffset) == true
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
    expression: Expression,
    frame: ComponentDescriber.ChangeFrame.Scale,
    references: TypeVariableReferences,
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
  val change = instruction as? Instruction.Change ?: return null
  val verb =
      when (change) {
        is Gain -> "raise"
        is Remove -> "lower"
        is Transmute -> return null
      }
  val modalVerb =
      when (change.intensity.modality()) {
        Modality.REQUIRED -> verb
        Modality.OPTIONAL -> "may $verb"
        Modality.BEST_EFFORT -> return null
      }
  val count = change.count.fixedQuantity() ?: return null
  val steps = if (count == 1) "step" else "steps"
  val subject =
      when {
        expression.simple -> NounPhrase.text(frame.subject)
        references.variableUsedWithin(expression) != null ->
            NounPhrase(frame.subject, determiner = Determiner.THAT)
        else -> return null
      }
  val predicate =
      Predicate(
          Verb(modalVerb),
          Coordination.one(subject.withModifier(Modifier.Phrase("$count $steps"))),
      )
  return Clause.Simple(predicate, NounPhrase.you().takeIf { modalVerb != verb })
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

private fun Describers.renderCardResourceHolder(expression: Expression): NounPhrase? {
  return renderCardResourceHolder(expression, owned = false)
}

private fun Describers.renderOwnedCardResourceHolder(expression: Expression): NounPhrase? {
  return renderCardResourceHolder(expression, owned = true)
}

private fun Describers.renderCardResourceHolder(
    expression: Expression,
    owned: Boolean,
): NounPhrase? {
  val resolved = resolveExpression(expression) ?: return null
  if (resolved.sourceDependencies.isNotEmpty() || expression.complement) return null
  val holder = fact(expression.className, ComponentDescriber::cardResourceHolder) ?: return null
  val refinement = expression.refinement ?: return null
  if (refinement.forgiving) return null
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  val metric = minimum.metric as? Metric.Count ?: return null
  if (!metric.expression.simple) return null
  tagName(metric.expression.className)?.let { tag ->
    if (minimum.target != 1) return null
    return if (owned) {
      NounPhrase.text("one of your $tag ${holder.plural}")
    } else {
      NounPhrase("$tag ${holder.singular}", determiner = Determiner.INDEFINITE)
    }
  }
  val resource =
      cardResourceNoun(metric.expression.className, maxOf(2, minimum.target)) ?: return null
  val subject =
      if (owned) {
        NounPhrase.text("one of your ${holder.plural}")
      } else {
        NounPhrase(holder.singular, determiner = Determiner.INDEFINITE)
      }
  return subject.withModifier(
      Modifier.Relation(
          "with",
          NounPhrase.text("${minimum.target} or more $resource on it"),
      )
  )
}

private fun clause(verb: String, noun: NounPhrase, vararg modifiers: Modifier): Clause.Simple =
    Clause.Simple(Predicate(Verb(verb), Coordination.one(noun), modifiers.toList()))

private val CLASS = cn("Class")
private val STANDARD_RESOURCE = cn("StandardResource")
