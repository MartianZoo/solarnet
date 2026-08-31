package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.text.ComponentDescriber.TriggerFrame as TriggerFrame

internal fun renderInstructionTree(
    instructionTree: InstructionTree,
    describers: Describers,
): Rendering<String> {
  val rendered = renderInstructions(instructionTree, describers)
  return Rendering(rendered.asSentences(), rendered.unresolved)
}

internal fun renderInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
): RenderedInstructions =
    renderLoweredInstructions(lowerProductionSyntax(instructionTree), describers)

private fun renderLoweredInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
): RenderedInstructions {
  val instructions = InstructionGroup.of(instructionTree).instructions
  if (instructions.isEmpty()) {
    return RenderedInstructions(
        listOf(Clause.Simple(Predicate("do", Coordination.one(NounPhrase.text("nothing")))))
    )
  }
  val rendered = instructions.flatMap { instruction ->
    val rendering = renderInstructionClauses(instruction, describers)
    val clauses =
        rendering.value
            ?: listOf(
                Clause.RawPets(
                    rendering.unresolved.singleOrNull()
                        ?: Unresolved(instruction, instructionRefusalReason(instruction))
                )
            )
    clauses.map { instruction to it }
  }
  return RenderedInstructions(coalesceAdjacentChanges(rendered, describers))
}

private fun renderInstructionClauses(
    instruction: Instruction,
    describers: Describers,
): Rendering<List<Clause>?> =
    if (instruction is Instruction.Transform) {
      Rendering.resolved(renderCardOperation(instruction, describers))
    } else {
      renderInstruction(instruction, describers).map { it?.let(::listOf) }
    }

private fun instructionRefusalReason(instruction: Instruction): RefusalReason =
    when (instruction) {
      is Gain,
      is Remove,
      is Instruction.Transmute -> RefusalReason.UNKNOWN_CHANGE_FRAME
      is Instruction.Or -> RefusalReason.UNSUPPORTED_ALTERNATIVES
      is Instruction.Per -> RefusalReason.UNSUPPORTED_SCALING
      is Instruction.Gated -> RefusalReason.UNSUPPORTED_GATE
      is Instruction.Then -> RefusalReason.UNSUPPORTED_SEQUENCE
      is Instruction.By,
      is Instruction.Transform -> RefusalReason.UNSUPPORTED_INSTRUCTION_KIND
      is NoOp -> error("NoOp is always renderable")
    }

private fun renderInstruction(
    instruction: Instruction,
    describers: Describers,
): Rendering<Clause?> =
    when (instruction) {
      is Gain,
      is Remove,
      is Instruction.Transmute -> renderChange(instruction, describers)
      is Instruction.Or -> Rendering.resolved(renderAlternatives(instruction, describers))
      is Instruction.Per -> Rendering.resolved(renderPer(instruction, describers))
      is Instruction.Gated -> Rendering.resolved(renderGated(instruction, describers))
      is Instruction.Then ->
          Rendering.resolved(
              renderPlacementBonusProductionSequence(instruction, describers)
                  ?: renderCardPlaySequence(instruction, describers)
                  ?: renderDiscardCostSequence(instruction, describers)
                  ?: renderCardResourceCostSequence(instruction, describers)
                  ?: renderSequentialThen(instruction, describers)
          )
      is NoOp ->
          Rendering.resolved(
              Clause.Simple(Predicate("do", Coordination.one(NounPhrase.text("nothing"))))
          )
      is Instruction.Transform -> error("Transforms are expanded before ordinary instructions")
      is Instruction.By -> Rendering.resolved(null)
    }

private fun renderDiscardCostSequence(
    instruction: Instruction.Then,
    describers: Describers,
): Clause.Simple? {
  val removal = instruction.stages.singleOrNull() as? Remove ?: return null
  val discarded = renderChange(removal, describers).value as? Clause.Simple ?: return null
  if (describers.changeFrame(removal.removing.className) !is ComponentDescriber.ChangeFrame.Deck) {
    return null
  }
  val result =
      renderLoweredInstructions(instruction.continuation, describers).clauses.singleOrNull()
          ?: return null
  return discarded.withModifier(Modifier.Phrase("to ${result.linearize()}"))
}

private fun renderSequentialThen(
    instruction: Instruction.Then,
    describers: Describers,
): Clause? {
  // TODO: Preserve Type Variable bindings when later stages refer to a choice made earlier.
  val clauses =
      (instruction.stages + instruction.continuation).map { part ->
        renderLoweredInstructions(part, describers).clauses.singleOrNull() ?: return null
      }
  return Clause.Coordinated(Coordination(clauses, Conjunction.THEN))
}

private fun renderPlacementBonusProductionSequence(
    instruction: Instruction.Then,
    describers: Describers,
): Clause? {
  val placement = instruction.stages.singleOrNull() as? Gain ?: return null
  if (
      placement.intensity.modality() != Modality.REQUIRED ||
          placement.gaining.refinement != null ||
          placement.gaining.complement ||
          placement.count.fixedQuantity() != 1
  ) {
    return null
  }
  val placementDescription = describers.positionedFrame(placement.gaining.className) ?: return null
  val alternatives = (instruction.continuation as? Instruction.Or)?.instructions ?: return null
  val bonuses = alternatives.map { alternative ->
    val gated = alternative as? Instruction.Gated ?: return null
    val minimum = gated.gate as? Requirement.Min ?: return null
    if (minimum.target != 1) return null
    val site = (minimum.metric as? Metric.Count)?.expression ?: return null
    val resolvedSite = describers.resolveExpression(site) ?: return null
    if (resolvedSite.sourceDependencies.isNotEmpty() || site.complement) return null
    val requirements =
        (site.refinement?.takeIf { !it.forgiving }?.requirement as? Requirement.And)?.requirements
            ?: return null
    val expressions = requirements.map { child ->
      val childMinimum = child as? Requirement.Min ?: return null
      if (childMinimum.target != 1) return null
      (childMinimum.metric as? Metric.Count)?.expression ?: return null
    }
    if (expressions.none { it.simple && it.className == placement.gaining.className }) return null
    val bonus =
        expressions.singleOrNull {
          describers.fact(it.className, ComponentDescriber::placementBonus) != null
        } ?: return null
    val bonusDescription =
        describers.fact(bonus.className, ComponentDescriber::placementBonus) ?: return null
    val resource = describers.representedClass(bonus) ?: return null
    val production =
        InstructionGroup.of(gated.inner).instructions.singleOrNull() as? Gain ?: return null
    if (
        production.intensity.modality() != Modality.REQUIRED ||
            production.count.fixedQuantity() != 1
    ) {
      return null
    }
    val produced = productionExpression(production.gaining, describers) ?: return null
    if (produced.owner != null || produced.resource != resource.className) return null
    PlacementBonusProduction(site.className, resource.className, bonusDescription.noun)
  }
  val siteClassName =
      bonuses.map(PlacementBonusProduction::siteClassName).distinct().singleOrNull() ?: return null
  val bonusNoun =
      bonuses.map(PlacementBonusProduction::bonusNoun).distinct().singleOrNull() ?: return null
  val resolvedPlacement = resolvePlacementExpression(placement.gaining, describers) ?: return null
  if (resolvedPlacement.owner != null || resolvedPlacement.unknownDependencies.isNotEmpty()) {
    return null
  }
  val siteModifiers =
      renderPlacementSites(resolvedPlacement, describers)?.takeIf { it.isNotEmpty() }
          ?: run {
            val description = describers.placementSite(siteClassName) ?: return null
            val noun = describers.describedNoun(siteClassName, description.noun, 1)
            val article = description.article ?: describers.indefiniteArticle(noun)
            listOf(Modifier.Phrase("on $article $noun"))
          }
  val resourceNames = bonuses.map { describers.componentNoun(it.resource, 1) }
  val resourceAlternatives = englishAlternatives(resourceNames)
  val bonusModifier =
      Modifier.Phrase(
          "with ${describers.indefiniteArticle(resourceAlternatives)} $resourceAlternatives " +
              bonusNoun.singular
      )
  val placed =
      Clause.Simple(
          Predicate(
              "place",
              Coordination.one(
                  NounPhrase(
                      placementDescription.singular,
                      placementDescription.plural,
                      determiner = placementDescription.article,
                  )
              ),
              siteModifiers.take(1) + bonusModifier + siteModifiers.drop(1),
          )
      )
  val production =
      Clause.Simple(
          Predicate(
              "increase",
              Coordination.one(NounPhrase.text("the matching production 1 step")),
          )
      )
  return Clause.Coordinated(Coordination(listOf(placed, production), Conjunction.AND))
}

private data class PlacementBonusProduction(
    val siteClassName: ClassName,
    val resource: ClassName,
    val bonusNoun: ComponentDescriber.Noun.Counted,
)

private fun renderCardResourceCostSequence(
    instruction: Instruction.Then,
    describers: Describers,
): Clause.Simple? {
  val removal = instruction.stages.singleOrNull() as? Remove ?: return null
  val resolved = describers.resolveCardResource(removal.removing) ?: return null
  if (
      removal.intensity.modality() != Modality.REQUIRED ||
          !describers.cardResourceHasHolder(resolved, describers.thisExpression) ||
          removal.removing.refinement != null ||
          removal.removing.complement
  ) {
    return null
  }
  val count = removal.count.fixedQuantity() ?: return null
  val resource = describers.cardResourceNounPhrase(removal.removing.className, count) ?: return null
  val result =
      renderLoweredInstructions(instruction.continuation, describers).clauses.singleOrNull()
          ?: return null
  return Clause.Simple(
      Predicate(
          "remove",
          Coordination.one(resource),
          listOf(
              Modifier.Phrase("from this card"),
              Modifier.Phrase("to ${result.linearize()}"),
          ),
      )
  )
}

private fun renderCardPlaySequence(
    instruction: Instruction.Then,
    describers: Describers,
): Clause.Simple? {
  val play = instruction.stages.singleOrNull() as? Gain ?: return null
  if (
      play.intensity.modality() != Modality.REQUIRED ||
          play.count.fixedQuantity() != 1 ||
          describers.triggerFrame(play.gaining.className) !is TriggerFrame.PlayCard ||
          (!play.gaining.simple && describers.representedExpression(play.gaining)?.simple != true)
  ) {
    return null
  }
  val modifier =
      when (val continuation = instruction.continuation) {
        is Instruction.Per -> {
          val removal = continuation.inner as? Remove ?: return null
          val counted = continuation.metric as? Metric.Count ?: return null
          if (
              removal.intensity.modality() != Modality.REQUIRED ||
                  !removal.removing.simple ||
                  removal.removing != counted.expression ||
                  removal.count.fixedQuantity() != 1 ||
                  describers.fact(
                      removal.removing.className,
                      ComponentDescriber::requirementShortfall,
                  ) != true
          ) {
            return null
          }
          "ignoring global requirements"
        }
        else -> {
          val reduction = maximumOwedReduction(continuation, describers) ?: return null
          "reducing its cost by ${reduction.count} ${reduction.noun}"
        }
      }
  return Clause.Simple(
      Predicate(
          "play",
          Coordination.one(NounPhrase.text("a card from hand")),
          listOf(Modifier.Supplement(modifier)),
      )
  )
}

private fun renderGated(
    instruction: Instruction.Gated,
    describers: Describers,
): Clause? {
  val clause =
      renderLoweredInstructions(instruction.inner, describers).clauses.singleOrNull() ?: return null
  val selectedClass =
      (instruction.gate as? Requirement.Min)
          ?.takeIf { it.minimum == 1 }
          ?.countedMetric
          ?.let { it as? Metric.Count }
          ?.expression
          ?.let(describers::representedClassArgument)
  if (
      selectedClass != null &&
          describers.changeFrame(selectedClass.className) is
              ComponentDescriber.ChangeFrame.Procedure
  ) {
    return clause
  }
  val condition = describers.renderGateCondition(instruction.gate) ?: return null
  return Clause.Prefaced(condition, clause)
}

private fun renderPer(
    instruction: Instruction.Per,
    describers: Describers,
): Clause? {
  val clause =
      renderLoweredInstructions(instruction.inner, describers).clauses.singleOrNull() ?: return null
  val metric = renderMetricPhrase(instruction.metric, describers) ?: return null
  return (clause as? Clause.Simple)?.withModifier(Modifier.Phrase("for $metric"))
}

private fun renderAlternatives(
    instruction: Instruction.Or,
    describers: Describers,
): Clause? {
  renderPlacementSiteFallback(instruction, describers)?.let {
    return it
  }
  val alternatives =
      instruction.instructions.map { option ->
        renderLoweredInstructions(option, describers).clauses.singleOrNull() ?: return null
      }
  if (alternatives.size == 2) {
    val firstAction = alternatives.singleOrNull {
      it is Clause.Prefaced && it.preface == "as your first action"
    }
    val decline = alternatives.singleOrNull { it !== firstAction }
    if (firstAction != null && decline.isDoNothing()) return firstAction
  }
  val simpleAlternatives = alternatives.map { it as? Clause.Simple }
  if (simpleAlternatives.all { it != null }) {
    coordinateClauseObjects(simpleAlternatives.filterNotNull(), Conjunction.OR)?.let {
      return it
    }
  }
  val conjunction =
      if (
          InstructionGroup.of(instruction.instructions.last()).instructions.isEmpty() ||
              alternatives.any { it is Clause.Prefaced }
      ) {
        Conjunction.COMMA_OR
      } else {
        Conjunction.OR
      }
  return Clause.Coordinated(Coordination(alternatives, conjunction))
}

private fun Clause?.isDoNothing(): Boolean =
    this is Clause.Simple &&
        predicate.verb == "do" &&
        predicate.objects?.members?.singleOrNull()?.linearize() == "nothing"

private fun renderPlacementSiteFallback(
    instruction: Instruction.Or,
    describers: Describers,
): Clause.Simple? {
  if (instruction.instructions.size != 2) return null
  val preferred =
      InstructionGroup.of(instruction.instructions.first()).instructions.singleOrNull() as? Gain
          ?: return null
  val fallback =
      InstructionGroup.of(instruction.instructions.last()).instructions.singleOrNull()
          as? Instruction.Gated ?: return null
  val unrestricted =
      InstructionGroup.of(fallback.inner).instructions.singleOrNull() as? Gain ?: return null
  val preferredPlacement = resolvePlacementExpression(preferred.gaining, describers) ?: return null
  val unrestrictedPlacement =
      resolvePlacementExpression(unrestricted.gaining, describers) ?: return null
  if (
      preferred.gaining.refinement != null ||
          preferred.gaining.complement ||
          preferred.gaining.className != unrestricted.gaining.className ||
          preferredPlacement.owner != null ||
          preferredPlacement.unknownDependencies.isNotEmpty() ||
          unrestrictedPlacement.owner != null ||
          unrestrictedPlacement.sites.isNotEmpty() ||
          unrestrictedPlacement.unknownDependencies.isNotEmpty() ||
          unrestricted.gaining.refinement != null ||
          unrestricted.gaining.complement ||
          preferred.intensity.modality() != unrestricted.intensity.modality() ||
          preferred.count != unrestricted.count ||
          preferred.count.fixedQuantity() != 1
  ) {
    return null
  }
  val site = preferredPlacement.sites.singleOrNull()?.takeIf { it.simple } ?: return null
  if (describers.placementSite(site.className) == null) return null
  if (describers.positionedFrame(preferred.gaining.className) == null) return null
  val absence = fallback.gate as? Requirement.Max ?: return null
  val countedSite = absence.countedMetric as? Metric.Count ?: return null
  if (absence.maximum != 0 || countedSite.expression != site) return null

  val preferredClause = renderChange(preferred, describers).value as? Clause.Simple ?: return null
  if (renderChange(unrestricted, describers).value !is Clause.Simple) return null
  return preferredClause
      .withModifier(Modifier.Phrase("if using a board that has one"))
      .withModifier(Modifier.Supplement("otherwise place it normally"))
}

private fun coalesceAdjacentChanges(
    rendered: List<Pair<Instruction, Clause>>,
    describers: Describers,
): List<Clause> {
  val result = mutableListOf<Clause>()
  var index = 0
  while (index < rendered.size) {
    val (instruction, renderedClause) = rendered[index]
    if (isProductionChange(instruction, describers)) {
      val run =
          rendered.drop(index).takeWhile { (candidate) ->
            isProductionChange(candidate, describers)
          }
      val clauses = factorAdjacentPredicates(run.map { it.second })
      result +=
          if (clauses.size == 1) clauses.single()
          else Clause.Coordinated(Coordination(clauses, Conjunction.AND))
      index += run.size
      continue
    }
    if (isCoalescibleStandardResourceGain(instruction, describers)) {
      val run =
          rendered.drop(index).takeWhile { (candidate) ->
            isCoalescibleStandardResourceGain(candidate, describers)
          }
      val clauses = factorAdjacentPredicates(run.map { it.second })
      result += clauses
      index += run.size
      continue
    }
    result += renderedClause
    index++
  }
  return result
}

private fun factorAdjacentPredicates(clauses: List<Clause>): List<Clause> {
  val result = mutableListOf<Clause>()
  clauses.forEach { clause ->
    val previous = result.lastOrNull() as? Clause.Simple
    val current = clause as? Clause.Simple
    val factored =
        if (previous != null && current != null) {
          coordinateClauseObjects(listOf(previous, current), Conjunction.AND)
        } else {
          null
        }
    if (factored != null) {
      result[result.lastIndex] = factored
    } else {
      result += clause
    }
  }
  return result
}

internal fun Describers.renderGateCondition(requirement: Requirement): String? {
  val counting = requirement as? Requirement.Counting ?: return null
  val metric = counting.metric as? Metric.Count ?: return null
  val expression = metric.expression
  val resolved = resolveExpression(expression) ?: return null
  if (
      resolved.sourceDependencies.isNotEmpty() ||
          expression.refinement != null ||
          expression.complement
  ) {
    return null
  }
  if (
      requirement is Requirement.Exact &&
          requirement.expected == 1 &&
          isGameParticipant(expression.className)
  ) {
    return "if this is a solo game"
  }
  if (
      requirement is Requirement.Max &&
          requirement.maximum == 0 &&
          isStandardResource(expression.className)
  ) {
    return "if you have no ${componentNoun(expression.className, 2)}"
  }
  if (requirement !is Requirement.Min) return null
  fact(expression.className, ComponentDescriber::presenceCondition)?.let { condition ->
    if (requirement.minimum != 1) return null
    val scope =
        if (isGenerationScoped(expression.className)) {
          " this generation"
        } else {
          ""
        }
    return "if $condition$scope"
  }
  fact(expression.className, ComponentDescriber::requirement)?.minimum?.let { bound ->
    if (bound is ComponentDescriber.Requirement.Bound.Count) {
      val noun = if (requirement.minimum == 1) bound.noun.singular else bound.noun.plural
      return if (isPlayerOwned(expression.className)) {
        "if you have ${requirement.minimum} $noun"
      } else {
        "if there are ${requirement.minimum} $noun"
      }
    }
  }
  val (name) = tagName(expression.className) ?: return null
  val tags = if (requirement.minimum == 1) "tag" else "tags"
  return "if you have ${requirement.minimum} $name $tags"
}
