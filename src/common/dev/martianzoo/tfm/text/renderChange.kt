package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.CUSTOM_INSTRUCTION
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
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.Dependency.Key

/** Interprets one Pets state change from passive component construction facts. */
internal fun renderChange(
    instruction: Instruction,
    describers: Describers,
    references: TypeVariableReferences = TypeVariableReferences.EMPTY,
): Clause? {
  val expression =
      when (instruction) {
        is Gain -> instruction.gaining
        is Remove -> instruction.removing
        is Transmute -> instruction.gaining
        else -> return null
      }
  val change = instruction as? Instruction.Change ?: return null
  if (change.quantifier.modality() != Modality.BEST_EFFORT) {
    return renderChangeOrNull(instruction, expression, describers, references)
  }
  val mandatory =
      when (change) {
        is Gain -> change.copy(quantifier = Instruction.Quantifier.MANDATORY)
        is Remove -> Remove.remove(change.scaledEx, Instruction.Quantifier.MANDATORY)
        is Transmute -> change.copy(quantifier = Instruction.Quantifier.MANDATORY)
      }
  val rendered =
      renderChangeOrNull(mandatory, expression, describers, references) as? Clause.Simple
          ?: return null
  return rendered.withModifier(Modifier.BEST_EFFORT)
}

internal fun changeRefusalReason(
    instruction: Instruction,
    describers: Describers,
): RefusalReason {
  val expression =
      when (instruction) {
        is Gain -> instruction.gaining
        is Remove -> instruction.removing
        is Transmute -> instruction.gaining
        else -> return RefusalReason.UNKNOWN_CHANGE_FRAME
      }
  return changeRefusalReason(instruction, expression, describers)
}

private fun renderChangeOrNull(
    instruction: Instruction,
    expression: Expression,
    describers: Describers,
    references: TypeVariableReferences,
): Clause? {
  if (instruction is Gain) {
    renderPlayPrelude(instruction, describers)?.let {
      return it
    }
    renderCardSearch(instruction, describers)?.let {
      return it
    }
  }
  if (instruction is Transmute) {
    renderCardMove(instruction, describers, references)?.let {
      return it
    }
    renderResourceConversion(instruction, describers, references)?.let {
      return it
    }
    renderCardBackExchange(instruction, describers)?.let {
      return it
    }
    renderCardResourceDrawExchange(instruction, describers)?.let {
      return it
    }
    renderPositionedConversion(instruction, describers, references)?.let {
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
      return renderProductionChange(instruction, describers, references)
  describers.changeFrame(expression.className)?.let { frame ->
    return when (frame) {
      ComponentDescriber.ChangeFrame.Countable ->
          renderCountableChange(instruction, describers, references)
      is ComponentDescriber.ChangeFrame.Held -> renderCardResourceChange(instruction, describers)
      is ComponentDescriber.ChangeFrame.Scale ->
          renderScaleChange(instruction, expression, frame, references)
      is ComponentDescriber.ChangeFrame.Positioned ->
          renderPlacement(instruction, frame, describers, references)
      ComponentDescriber.ChangeFrame.Deck ->
          renderCardKeep(instruction, describers)
              ?: renderDiscard(instruction, describers)
              ?: renderCardDrawOrLook(instruction, describers)
      is ComponentDescriber.ChangeFrame.Procedure ->
          renderProcedure(instruction, frame, describers, references)
      is ComponentDescriber.ChangeFrame.State -> renderStateChange(instruction, frame, describers)
      is ComponentDescriber.ChangeFrame.ScopedInstruction -> null
      ComponentDescriber.ChangeFrame.RequiredAction -> renderRequiredAction(instruction, describers)
      ComponentDescriber.ChangeFrame.NextCardEffect -> renderNextCardEffect(instruction, describers)
      ComponentDescriber.ChangeFrame.Play -> renderCardPlay(instruction, describers)
    }
  }
  return null
}

private fun renderCardSearch(gain: Gain, describers: Describers): Clause.Simple? {
  if (gain.quantifier.modality() != Modality.REQUIRED) return null
  val search = gain.gaining
  if (search.className != cn("SearchForCard") || search.refinement != null) return null
  val filter = search.arguments.singleOrNull() ?: return null
  val criterion = describers.cardFilterCriterion(filter) ?: return null
  val count = gain.count.fixedQuantity() ?: return null
  return clause(
      "draw",
      NounPhrase.text("$count ${matchingCardNoun(criterion, count == 1, describers)}"),
  )
}

private fun renderTypeVariableResourceChange(
    instruction: Instruction,
    expression: Expression,
    describers: Describers,
    references: TypeVariableReferences,
): Clause.Simple? {
  val change = instruction as? Instruction.Change ?: return null
  if (
      change.quantifier.modality() != Modality.REQUIRED ||
          !expression.simple ||
          expression.refinement != null
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
  if (expression.refinement != null) {
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
    is ComponentDescriber.ChangeFrame.State,
    is ComponentDescriber.ChangeFrame.ScopedInstruction,
    ComponentDescriber.ChangeFrame.RequiredAction,
    ComponentDescriber.ChangeFrame.NextCardEffect,
    ComponentDescriber.ChangeFrame.Play -> RefusalReason.UNSUPPORTED_DECLARED_CHANGE
    null -> RefusalReason.UNKNOWN_CHANGE_FRAME
  }
}

private fun renderStateChange(
    instruction: Instruction,
    frame: ComponentDescriber.ChangeFrame.State,
    describers: Describers,
): Clause.Simple? {
  val change = instruction as? Instruction.Change ?: return null
  if (change.quantifier.modality() != Modality.REQUIRED || change.count.fixedQuantity() != 1) {
    return null
  }
  val ownerKey = Key(OWNED, 0)
  val procedure =
      if (change is Transmute) {
        if (
            change.gaining.className != change.removing.className ||
                change.gaining.refinement != null ||
                change.removing.refinement != null
        ) {
          return null
        }
        val gaining = describers.resolveExpression(change.gaining) ?: return null
        val removing = describers.resolveExpression(change.removing) ?: return null
        if (!gaining.hasOnlySourceDependency(ownerKey, describers.ownerExpression)) return null
        val previousOwner =
            removing.sourceDependency(ownerKey)?.takeIf(Expression::simple) ?: return null
        if (removing.sourceDependencies.keys != setOf(ownerKey)) return null
        frame.ownershipTransfers[previousOwner.className]
      } else {
        val expression = change.gaining ?: change.removing ?: return null
        if (
            expression.refinement != null ||
                describers.resolveExpression(expression)?.sourceDependencies?.keys?.any {
                  it != ownerKey
                } != false
        ) {
          return null
        }
        when (change) {
          is Gain -> frame.enter
          is Remove -> frame.leave
          is Transmute -> error("Handled above")
        }
      } ?: return null
  if (procedure.cardTargetRelation != null) return null
  return procedure.objectPhrase?.let { clause(procedure.verb, NounPhrase.text(it)) }
      ?: Clause.Simple(Predicate(Verb(procedure.verb)))
}

/** A retained card moves from the selection into the same player's hand. */
private fun renderCardKeep(instruction: Instruction, describers: Describers): Clause? {
  val move = instruction as? Transmute ?: return null
  if (move.quantifier.modality() != Modality.REQUIRED) return null
  val count = move.count.fixedQuantity() ?: return null
  val gaining = move.gaining
  val removing = move.removing
  if (
      gaining.className != removing.className ||
          gaining.refinement != null ||
          removing.refinement != null
  )
      return null
  val locationKey =
      describers.fact(gaining.className, ComponentDescriber::cardLocationDependency) ?: return null
  val destination = describers.resolveExpression(gaining) ?: return null
  val source = describers.resolveExpression(removing) ?: return null
  if (
      destination.sourceDependencies.keys.any { it != locationKey } ||
          source.sourceDependencies.keys.any { it != locationKey } ||
          describers.location(gaining) != ComponentDescriber.CardLocation.HAND ||
          describers.location(removing) != ComponentDescriber.CardLocation.SELECTING
  )
      return null
  return clause("keep", describers.quantifiedComponentNounPhrase(gaining.className, count))
      .withModifier(Modifier.Relation("from", NounPhrase.text("the selection")))
}

private fun renderDiscard(
    instruction: Instruction,
    describers: Describers,
): Clause? {
  val removal = instruction as? Remove ?: return null
  if (removal.quantifier.modality() != Modality.REQUIRED) return null
  if (!removal.removing.simple) return null
  val count = removal.count.fixedQuantity() ?: return null
  return clause(
      "discard",
      describers.quantifiedComponentNounPhrase(removal.removing.className, count),
  )
}

private fun renderCardDrawOrLook(
    instruction: Instruction,
    describers: Describers,
): Clause.Simple? {
  val gain = instruction as? Gain ?: return null
  if (
      gain.quantifier.modality() == Modality.REQUIRED &&
          gain.gaining.refinement == null &&
          describers.location(gain.gaining) == ComponentDescriber.CardLocation.SELECTING
  ) {
    val locationKey =
        describers.fact(gain.gaining.className, ComponentDescriber::cardLocationDependency)
            ?: return null
    val resolved = describers.resolveExpression(gain.gaining) ?: return null
    if (resolved.sourceDependencies.keys.any { it != locationKey }) return null
    val count = gain.count.fixedQuantity() ?: return null
    return clause(
        "look at",
        NounPhrase.text("$count ${describers.componentNoun(gain.gaining.className, count)}"),
    )
  }
  if (
      gain.quantifier.modality() != Modality.REQUIRED ||
          !gain.gaining.simple ||
          !describers.concrete(gain.gaining.className)
  ) {
    return null
  }
  val count = gain.count.fixedQuantity() ?: return null
  return clause("draw", describers.quantifiedComponentNounPhrase(gain.gaining.className, count))
}

private fun renderProcedure(
    instruction: Instruction,
    frame: ComponentDescriber.ChangeFrame.Procedure,
    describers: Describers,
    references: TypeVariableReferences,
): Clause.Simple? {
  val gain = instruction as? Gain ?: return null
  if (gain.quantifier.modality() != Modality.REQUIRED || gain.count.fixedQuantity() != 1) {
    return null
  }
  val objectPhrase = frame.objectPhrase ?: return Clause.Simple(Predicate(Verb(frame.verb)))
  val targetRelation = frame.cardTargetRelation
  if (targetRelation == null) {
    if (!gain.gaining.simple) return null
    return clause(frame.verb, NounPhrase.text(objectPhrase))
  }
  if (gain.gaining.refinement != null) return null
  val target =
      gain.gaining.arguments.singleOrNull()?.let { describers.cardSelector(it, references) }
          ?: return null
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
  if (gain.quantifier.modality() != Modality.REQUIRED || gain.count.fixedQuantity() != 1)
      return null
  val action =
      describers.actionUseEvent(Effect.Trigger.OnGainOf.create(gain.gaining))?.provider
          ?: return null
  return describers.renderActionUse(action)?.let { clause("use", it) }
}

private fun renderPositionedConversion(
    transmute: Transmute,
    describers: Describers,
    references: TypeVariableReferences,
): Clause.Simple? {
  if (transmute.quantifier.modality() != Modality.REQUIRED) return null
  if (transmute.count.fixedQuantity() != 1) return null
  val gaining = transmute.gaining
  val removing = transmute.removing
  if (gaining.refinement != null || removing.refinement != null) return null
  val gainingFrame = describers.positionedFrame(gaining.className) ?: return null
  val removingFrame = describers.positionedFrame(removing.className) ?: return null
  val gainingPlacement = resolvePlacementExpression(gaining, describers) ?: return null
  val removingPlacement = resolvePlacementExpression(removing, describers) ?: return null
  if (
      gainingPlacement.unknownDependencies.isNotEmpty() ||
          removingPlacement.unknownDependencies.isNotEmpty()
  )
      return null
  if (gainingPlacement.owner == describers.anyoneExpression) return null
  val site = gainingPlacement.sites.singleOrNull() ?: return null
  val sourceSite = removingPlacement.sites.singleOrNull() ?: return null
  val sourceModifiers =
      renderPlacementSites(removingPlacement, describers, siteDeterminer = Determiner.ANY)
          ?: return null
  val destinationModifiers =
      renderPlacementSites(
          gainingPlacement,
          describers,
          siteDeterminer =
              Determiner.THAT.takeIf {
                sourceSite === site || references.sameVariable(sourceSite, site)
              },
      ) ?: return null
  val source =
      renderPlacedComponentNoun(
          removingPlacement,
          1,
          removingFrame,
          describers,
          explicitOwnership = true,
      ) ?: return null
  val destination =
      renderPlacedComponentNoun(
          gainingPlacement,
          1,
          gainingFrame,
          describers,
          explicitOwnership = true,
      ) ?: return null
  return clause(
      "change",
      sourceModifiers.fold(source, NounPhrase::withModifier),
      Modifier.Relation("into", destinationModifiers.fold(destination, NounPhrase::withModifier)),
  )
}

private fun renderCardPlay(instruction: Instruction, describers: Describers): Clause.Simple? {
  val gain = instruction as? Gain ?: return null
  if (
      gain.quantifier.modality() != Modality.REQUIRED ||
          gain.gaining.refinement != null ||
          gain.count.fixedQuantity() != 1
  ) {
    return null
  }
  val resolved = describers.resolveExpression(gain.gaining) ?: return null
  val play = cn("PlayCard")
  val backKey = Key(play, 0)
  val faceKey = Key(play, 1)
  val locationKey = Key(play, 2)
  if (resolved.sourceDependencies.keys.any { it !in setOf(backKey, faceKey, locationKey) }) {
    return null
  }
  val back = resolved.dependency(backKey) ?: return null
  val face = resolved.dependency(faceKey) ?: return null
  if (back.refinement != null || face.refinement != null) return null
  if (face.representedClass?.className != cn("CardFront")) return null
  val card = back.representedClass ?: return null
  var noun =
      describers
          .componentNounPhrase(card.className, 1)
          .copy(count = null, determiner = Determiner.INDEFINITE)
  if (locationKey in resolved.selectedKeys) {
    val source =
        when (describers.location(gain.gaining)) {
          ComponentDescriber.CardLocation.HAND -> "hand"
          ComponentDescriber.CardLocation.SELECTING -> "the selection"
          ComponentDescriber.CardLocation.REVEALED -> "the revealed cards"
          null -> return null
        }
    noun = noun.withModifier(Modifier.Relation("from", NounPhrase.text(source)))
  }
  return clause("play", noun)
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
  val result = renderInstructions(effect.instruction, describers).asCoordinatedClause()
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
          declaration.customMetric ||
          declaration.supertypes.any { it.className == CUSTOM_INSTRUCTION } ||
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
): Clause? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  if (count != 1) return null
  val declaration = behaviorSubclassDeclaration(className, describers) ?: return null
  if (declaration.authoredEffectsWithActions.size != 1) return null
  val effect =
      declaration.authoredEffects.singleOrNull()?.let(describers::prepareForRendering)
          ?: return null
  paymentDiscount(effect, describers)?.let { discount ->
    if (discount.reduction.count == 0) return null
    val trigger =
        Clause.Simple(
            subject = NounPhrase.you(),
            predicate =
                Predicate(
                    Verb("play"),
                    Coordination.one(NounPhrase.text("the next card this generation")),
                ),
        )
    val result =
        Clause.Simple(
            subject = NounPhrase.you(),
            predicate =
                Predicate(
                    Verb("pay"),
                    Coordination.one(
                        discount.reduction.phrase.withModifier(Modifier.Phrase("less"))
                    ),
                    listOf(Modifier.Phrase("for it")),
                ),
        )
    return Clause.Prefaced(
        Clause.Preface.Temporal(trigger),
        result,
    )
  }
  return renderRequirementFlexibilityResult(
      effect,
      describers,
      NounPhrase.text("the next card you play this generation"),
  )
}

/** A card's identity is retained when its front becomes a played event. */
private fun renderCardMove(
    change: Transmute,
    describers: Describers,
    references: TypeVariableReferences,
): Clause? {
  if (change.quantifier.modality() != Modality.REQUIRED || change.count.fixedQuantity() != 1)
      return null
  val destination = change.gaining
  if (destination.className != cn("PlayedEvent") || destination.refinement != null) return null
  val representation =
      destination.arguments.singleOrNull { it.className == cn("Class") } ?: return null
  if (representation.refinement != null) return null
  val represented = representation.arguments.singleOrNull() ?: return null
  if (represented != change.removing) return null
  val card =
      if (change.removing == describers.thisExpression)
          NounPhrase("card", determiner = Determiner.THIS)
      else describers.cardSelector(change.removing, references) ?: return null
  val owner = destination.arguments.filterNot { it === representation }.singleOrNull()
  if (destination.arguments.size > 2) return null
  val pile =
      when {
        owner == null || owner == describers.ownerExpression -> "your event pile"
        references.variableUsedAt(owner) != null -> "that player's event pile"
        else -> return null
      }
  return clause("move", card, Modifier.Relation("to", NounPhrase.text(pile)))
}

/** Different resource kinds are a conversion; resource-to-scale changes are a cost and result. */
private fun renderResourceConversion(
    change: Transmute,
    describers: Describers,
    references: TypeVariableReferences,
): Clause? {
  val count = change.count.fixedQuantity() ?: return null
  val modality = change.quantifier.modality()
  if (modality == Modality.BEST_EFFORT || modality == Modality.OPTIONAL && count != 1) return null
  val from = change.removing
  val to = change.gaining
  if (
      from.simple &&
          to.simple &&
          from.className != to.className &&
          describers.isStandardResource(from.className) &&
          describers.isStandardResource(to.className)
  ) {
    return Clause.Simple(
        Predicate(
            Verb(if (modality == Modality.OPTIONAL) "may convert" else "convert"),
            Coordination.one(describers.componentNounPhrase(from.className, count)),
            listOf(Modifier.Relation("to", describers.componentNounPhrase(to.className, count))),
        ),
        NounPhrase.you().takeIf { modality == Modality.OPTIONAL },
    )
  }
  if (
      modality != Modality.REQUIRED ||
          from.className !in describers.expressions.classesByName ||
          !describers.isCardResource(from.className) ||
          describers.scaleFrame(to.className) == null
  )
      return null
  val removal =
      renderChange(
          Remove.remove(scaledEx(from, change.count)),
          describers,
          references,
      )
          as? Clause.Simple ?: return null
  val gain =
      renderChange(
          Gain.gain(scaledEx(to, change.count)),
          describers,
          references,
      ) ?: return null
  return attachPurpose(listOf(removal), gain)
}

private fun renderCardBackExchange(
    transmute: Transmute,
    describers: Describers,
): Clause? {
  if (transmute.quantifier.modality() != Modality.REQUIRED) return null
  val count = transmute.count.fixedQuantity() ?: return null
  val gained = transmute.gaining
  val removed = transmute.removing
  if (!gained.simple || !removed.simple) return null
  if (
      describers.changeFrame(gained.className) != ComponentDescriber.ChangeFrame.Deck ||
          describers.changeFrame(removed.className) != ComponentDescriber.ChangeFrame.Deck ||
          !describers.concrete(gained.className) ||
          !describers.concrete(removed.className)
  ) {
    return null
  }
  return attachPurpose(
      listOf(clause("discard", describers.quantifiedComponentNounPhrase(removed.className, count))),
      clause("draw", describers.quantifiedComponentNounPhrase(gained.className, count)),
  )
}

private fun renderCardResourceDrawExchange(
    transmute: Transmute,
    describers: Describers,
): Clause? {
  if (transmute.quantifier.modality() != Modality.REQUIRED) return null
  val count = transmute.count.fixedQuantity() ?: return null
  val gaining = transmute.gaining
  if (!gaining.simple || !describers.concrete(gaining.className)) return null
  val removing = transmute.removing
  val resolved = describers.resolveCardResource(removing) ?: return null
  if (
      !describers.cardResourceHasHolder(resolved, describers.thisExpression) ||
          removing.refinement != null
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
    references: TypeVariableReferences,
): Clause? {
  concreteMandatoryGain(instruction)?.let { (className, count) ->
    val noun = describers.componentNounPhrase(className, count)
    return clause("gain", noun)
  }
  (instruction as? Gain)?.let { gain ->
    if (
        gain.quantifier.modality() == Modality.OPTIONAL &&
            gain.gaining.simple &&
            describers.concrete(gain.gaining.className)
    ) {
      val count = gain.count.fixedQuantity() ?: return@let
      return Clause.Simple(
          Predicate(
              Verb("may gain"),
              Coordination.one(
                  describers.componentNounPhrase(gain.gaining.className, count).atMost()
              ),
          ),
          NounPhrase.you(),
      )
    }
  }
  (instruction as? Transmute)?.let {
    return renderStandardResourceTransfer(it, describers, references)
  }
  val removal = instruction as? Remove ?: return null
  val expression = removal.removing
  if (expression.refinement != null) return null
  val count = removal.count.fixedQuantity() ?: return null
  if (!describers.concrete(expression.className)) return null
  if (expression.simple) {
    val amount = describers.componentNounPhrase(expression.className, count)
    return when (removal.quantifier.modality()) {
      Modality.REQUIRED -> clause("lose", amount)
      Modality.BEST_EFFORT,
      Modality.OPTIONAL -> null
    }
  }
  val resolved = describers.resolveExpression(expression) ?: return null
  val ownerKey = Key(OWNED, 0)
  val player =
      resolved
          .sourceDependency(ownerKey)
          ?.takeIf { resolved.sourceDependencies.size == 1 }
          ?.let { describers.renderEligiblePlayer(it, references) }
  if (player != null && removal.quantifier.modality() == Modality.OPTIONAL) {
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

private fun Describers.renderEligiblePlayer(
    expression: Expression,
    references: TypeVariableReferences,
): NounPhrase? {
  if (expressions.isOwner(expression)) return null
  if (
      expression.refinement == null &&
          expression.arguments.isEmpty() &&
          (isGameParticipant(expression.className) ||
              expression.className == anyoneExpression.className) &&
          references.variableUsedAt(expression) != null
  ) {
    return NounPhrase("player", determiner = Determiner.THAT)
  }
  if (expression.copy(typeVariableName = null) == anyoneExpression) {
    return NounPhrase("player", determiner = Determiner.ANY)
  }
  if (expression == playerExpression) return NounPhrase("player", determiner = Determiner.THAT)
  if (
      expression.className != anyoneExpression.className ||
          resolveExpression(expression)?.sourceDependencies?.isNotEmpty() != false
  ) {
    return null
  }
  val refinement = expression.refinement as? Expression.Refinement.Has ?: return null
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  if (minimum.target != 1) return null
  val tagExpression = (minimum.metric as? Metric.Count)?.expression ?: return null
  if (
      tagExpression.refinement != null ||
          resolveExpression(tagExpression)?.let { tag ->
            tag.sourceDependencies.isNotEmpty() &&
                !tag.hasOnlySourceDependency(Key(OWNED, 0), anyoneExpression)
          } != false
  ) {
    return null
  }
  val tag = playedTagPhrase(tagExpression.className) ?: return null
  return NounPhrase("player", determiner = Determiner.INDEFINITE)
      .withModifier(
          Modifier.Relation(
              "with",
              tag.withModifier(Modifier.Phrase("in play")),
          )
      )
}

private fun renderStandardResourceTransfer(
    transmute: Transmute,
    describers: Describers,
    references: TypeVariableReferences,
): Clause? {
  val gaining = transmute.gaining
  val removing = transmute.removing
  if (gaining.className != removing.className) return null
  if (gaining.refinement != null || removing.refinement != null) {
    return null
  }
  if (!describers.concrete(gaining.className)) return null
  if (!describers.isStandardResource(gaining.className)) return null
  val recipient = renderTransferParty(gaining, describers, references) ?: return null
  val payer = renderTransferParty(removing, describers, references) ?: return null
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
  val amount = if (transmute.quantifier.modality() == Modality.OPTIONAL) noun.atMost() else noun
  val predicate =
      Predicate(
          Verb(if (transmute.quantifier.modality() == Modality.OPTIONAL) "may $verb" else verb),
          Coordination.one(amount),
          listOf(Modifier.Phrase("$preposition ${otherParty.objectPhrase}")),
      )
  return Clause.Simple(
      predicate,
      if (transmute.quantifier.modality() == Modality.OPTIONAL) NounPhrase.you() else null,
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
    references: TypeVariableReferences,
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
    resolved.sourceDependencies.size == 1 &&
        resolved.sourceDependency(ownerKey)?.let(references::variableUsedAt) != null ->
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
  if (expression.refinement != null) return null
  val count = change.count.fixedQuantity() ?: return null
  val noun = describers.cardResourceNounPhrase(expression.className, count) ?: return null
  val resolved = describers.resolveHeldResource(expression) ?: return null
  val holder = describers.heldResourceHolder(resolved)
  if (instruction is Remove) {
    if (
        resolved.hasOnlySourceDependency(Key(OWNED, 0), describers.anyoneExpression) &&
            change.quantifier.modality() == Modality.OPTIONAL
    ) {
      return Clause.Simple(
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
    }
    if (change.quantifier.modality() != Modality.REQUIRED) return null
    val source =
        when {
          resolved.sourceDependencies.isEmpty() -> NounPhrase("card", determiner = Determiner.ANY)
          describers.heldResourceHasHolder(resolved, describers.thisExpression) ->
              NounPhrase("card", determiner = Determiner.THIS)
          holder != null && describers.heldResourceHasHolder(resolved, holder) ->
              describers.renderCardResourceHolder(holder) ?: return null
          else -> return null
        }
    return clause("remove", noun, Modifier.Relation("from", source))
  }
  if (
      change.quantifier.modality() == Modality.OPTIONAL &&
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
  if (change.quantifier.modality() == Modality.OPTIONAL) return null
  val target =
      when {
        describers.heldResourceHasHolder(resolved, describers.thisExpression) ->
            NounPhrase("card", determiner = Determiner.THIS)
        holder != null && describers.heldResourceHasHolder(resolved, holder) ->
            describers.renderCardResourceHolder(holder) ?: return null
        resolved.sourceDependencies.isNotEmpty() -> return null
        else ->
            NounPhrase(
                "card",
                determiner = describers.unboundCardResourceDestination(expression.className),
            )
      }
  return clause("add", noun, Modifier.Relation("to", target))
}

private fun renderProductionChange(
    instruction: Instruction,
    describers: Describers,
    references: TypeVariableReferences,
): Clause? {
  val change = instruction as? Instruction.Change ?: return null
  if (change.quantifier.modality() != Modality.REQUIRED) return null
  val gaining =
      when (change) {
        is Gain -> true
        is Remove -> false
        is Transmute -> return renderProductionConversion(change, describers)
      }
  val expression = change.gaining ?: change.removing ?: return null
  renderSelectedProductionChange(change, gaining, expression, describers, references)?.let {
    return it
  }
  val production = simpleProductionChange(change, describers) ?: return null
  val determiner =
      when {
        production.owner == null -> Determiner.YOUR
        !production.gaining && production.owner == describers.anyoneExpression ->
            Determiner.ANY_PLAYER_POSSESSIVE
        else -> return null
      }
  return clause(
      if (production.gaining) "increase" else "decrease",
      describers
          .productionNounPhrase(production.resource)
          .withDeterminer(determiner)
          .withModifier(Modifier.Steps(production.count)),
  )
}

private fun renderSelectedProductionChange(
    change: Instruction.Change,
    gaining: Boolean,
    expression: Expression,
    describers: Describers,
    references: TypeVariableReferences,
): Clause.Simple? {
  if (expression.refinement != null) {
    return null
  }
  val resource = selectedProductionResource(expression, describers) ?: return null
  if (
      !describers.isStandardResource(resource.className) ||
          resource.refinement is Expression.Refinement.Not
  ) {
    return null
  }
  val refinement = resource.refinement as? Expression.Refinement.Has ?: return null
  val count = change.count.fixedQuantity() ?: return null
  renderMatchingPlacementBonus(refinement.requirement, describers, references)?.let { bonus ->
    return clause(
        if (gaining) "increase" else "decrease",
        describers
            .productionNounPhrase(resource.className)
            .withDeterminer(Determiner.YOUR)
            .withModifier(Modifier.Steps(count)),
        Modifier.Relation("matching", bonus),
    )
  }
  if (describers.concrete(resource.className)) return null
  val first = refinement.requirement as? Requirement.Exact ?: return null
  if (first.target != 1 || !first.metric.isLowestStandardProductionRank(describers)) return null
  return clause(
      if (gaining) "increase" else "decrease",
      NounPhrase.text("one of your lowest productions").withModifier(Modifier.Steps(count)),
  )
}

private fun renderMatchingPlacementBonus(
    requirement: Requirement,
    describers: Describers,
    references: TypeVariableReferences,
): NounPhrase? {
  val minimum = requirement as? Requirement.Min ?: return null
  if (minimum.target != 1) return null
  val expression = countedExpression(minimum) ?: return null
  if (expression.refinement != null) return null
  val bonus =
      describers.fact(expression.className, ComponentDescriber::placementBonus) ?: return null
  val placement = resolvePlacementExpression(expression, describers) ?: return null
  // The enclosing resource class supplies the bonus's resource; only its site is authored here.
  if (placement.unknownDependencies.isNotEmpty() || placement.owner != null) return null
  val modifiers =
      renderPlacementSites(placement, describers, references = references) ?: return null
  return modifiers.fold(NounPhrase(bonus.noun.singular, determiner = Determiner.INDEFINITE)) {
      noun,
      modifier ->
    noun.withModifier(modifier)
  }
}

private fun Metric.isLowestStandardProductionRank(describers: Describers): Boolean {
  val rank = this as? Metric.Rank ?: return false
  val selector = rank.selector ?: return false
  if (
      selector.className != CLASS ||
          selector.arguments.singleOrNull()?.takeIf { it.simple }?.className != STANDARD_RESOURCE ||
          selector.refinement is Expression.Refinement.Not
  ) {
    return false
  }
  val alternatives = (rank.metrics.singleOrNull() as? Metric.Or)?.metrics ?: return false
  if (alternatives.size != 2) return false
  val production =
      alternatives.singleOrNull { alternative ->
        val expression = alternative.expression
        val excluded = expression.arguments.singleOrNull()
        val exclusion = excluded?.refinement as? Expression.Refinement.Not
        describers.isProduction(expression.className) &&
            excluded?.className == selector.className &&
            excluded.arguments == selector.arguments &&
            exclusion != null &&
            sameNamedTypeVariable(exclusion.excluded, selector) &&
            expression.refinement == null
      } ?: return false
  val offset = alternatives.single { it != production }.expression
  return offset.arguments.singleOrNull()?.let { sameNamedTypeVariable(it, selector) } == true &&
      offset.refinement == null &&
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
          describers
              .productionNounPhrase(removing.resource)
              .withDeterminer(Determiner.YOUR)
              .withModifier(Modifier.Steps(Modifier.Steps.Amount.OneOrMore)),
      )
  val increase =
      clause(
          "increase",
          describers
              .productionNounPhrase(gaining.resource)
              .withDeterminer(Determiner.YOUR)
              .withModifier(Modifier.Steps(Modifier.Steps.Amount.SameNumber)),
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
    val gainingAnother =
        (instruction.gaining.refinement as? Expression.Refinement.Not)?.excluded?.let { excluded ->
          instruction.gaining.className == instruction.removing.className &&
              sameNamedTypeVariable(excluded, instruction.removing)
        } == true
    if (
        instruction.quantifier.modality() != Modality.REQUIRED ||
            !(instruction.gaining.simple && instruction.removing == instruction.gaining) &&
                !gainingAnother
    ) {
      return null
    }
    val count = instruction.count.fixedQuantity() ?: return null
    val increase = clause("increase", NounPhrase.text("one ${frame.subject} ${stepCount(count)}"))
    val decrease =
        clause("decrease", NounPhrase.text("another ${frame.subject} ${stepCount(count)}"))
    return Clause.Coordinated(Coordination(listOf(increase, decrease), Conjunction.AND))
  }
  val change = instruction as? Instruction.Change ?: return null
  val verb =
      when (change) {
        is Gain -> frame.increaseVerb
        is Remove -> frame.decreaseVerb
        is Transmute -> return null
      }
  val modalVerb =
      when (change.quantifier.modality()) {
        Modality.REQUIRED -> verb
        Modality.OPTIONAL -> "may $verb"
        Modality.BEST_EFFORT -> return null
      }
  val count = change.count.fixedQuantity() ?: return null
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
          Coordination.one(subject.withModifier(Modifier.Phrase(stepCount(count)))),
      )
  return Clause.Simple(predicate, NounPhrase.you().takeIf { modalVerb != verb })
}

private fun concreteMandatoryGain(instruction: Instruction): Pair<ClassName, Int>? {
  val gain = instruction as? Gain ?: return null
  if (gain.quantifier.modality() != Modality.REQUIRED) return null
  if (!gain.gaining.simple) return null
  val count = gain.count.fixedQuantity() ?: return null
  return gain.gaining.className to count
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
  if (resolved.sourceDependencies.isNotEmpty()) return null
  val holder = fact(expression.className, ComponentDescriber::cardResourceHolder) ?: return null
  val refinement = expression.refinement as? Expression.Refinement.Has ?: return null
  val criterion = cardCriterion(refinement.requirement)
  if (criterion is CardCriterion.Tag) {
    val tag = tagName(criterion.className) ?: return null
    return if (owned) {
      NounPhrase.text("one of your $tag ${holder.plural}")
    } else {
      NounPhrase("$tag ${holder.singular}", determiner = Determiner.INDEFINITE)
    }
  }
  val minimum = refinement.requirement as? Requirement.Min ?: return null
  val metric = minimum.metric as? Metric.Count ?: return null
  if (!metric.expression.simple) return null
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
