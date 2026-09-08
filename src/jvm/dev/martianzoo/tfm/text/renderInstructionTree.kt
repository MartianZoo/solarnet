package dev.martianzoo.tfm.text

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
  val prepared = describers.prepareForRendering(instructionTree)
  val rendered =
      renderPreparedInstructions(prepared, describers, TypeVariableReferences.from(prepared))
  return rendered.asSentences()
}

internal fun renderInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
): RenderedInstructions {
  val prepared = describers.prepareForRendering(instructionTree)
  return renderPreparedInstructions(prepared, describers, TypeVariableReferences.from(prepared))
}

internal fun renderPreparedInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
    references: TypeVariableReferences,
): RenderedInstructions = renderLoweredInstructions(instructionTree, describers, references)

private fun renderLoweredInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
    references: TypeVariableReferences,
): RenderedInstructions {
  val localReferences = references.including(instructionTree)
  val instructions = InstructionGroup.of(instructionTree).instructions
  if (instructions.isEmpty()) {
    return RenderedInstructions(listOf(doNothingClause))
  }
  val rendered = mutableListOf<Pair<Instruction, Clause>>()
  var index = 0
  while (index < instructions.size) {
    val instruction = instructions[index]
    val paired =
        instructions.getOrNull(index + 1)?.let { next ->
          renderAdjacentCardInstructions(instruction, next, describers)
        }
    if (paired != null) {
      rendered += paired
      index += 2
      continue
    }
    val rendering = renderInstructionClauses(instruction, describers, localReferences)
    val clauses =
        rendering.value
            ?: listOf(
                Clause.RawPets(
                    rendering.unresolved.singleOrNull()
                        ?: Unresolved(instruction, instructionRefusalReason(instruction))
                )
            )
    rendered += clauses.map { instruction to it }
    index++
  }
  return RenderedInstructions(coalesceAdjacentChanges(rendered, describers))
}

private fun renderInstructionClauses(
    instruction: Instruction,
    describers: Describers,
    references: TypeVariableReferences,
): Rendering<List<Clause>?> =
    if (instruction is Instruction.Transform) {
      Rendering.resolved(renderCardOperation(instruction, describers))
    } else {
      renderInstruction(instruction, describers, references).map { it?.let(::listOf) }
    }

private fun instructionRefusalReason(instruction: Instruction): RefusalReason =
    when (instruction) {
      is Gain,
      is Remove,
      is Instruction.Transmute -> RefusalReason.UNKNOWN_CHANGE_FRAME
      is Instruction.Each -> RefusalReason.UNSUPPORTED_FANOUT
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
    references: TypeVariableReferences,
): Rendering<Clause?> =
    when (instruction) {
      is Gain,
      is Remove,
      is Instruction.Transmute -> renderChange(instruction, describers, references)
      is Instruction.Each ->
          Rendering(null, listOf(Unresolved(instruction, RefusalReason.UNSUPPORTED_FANOUT)))
      is Instruction.Or ->
          Rendering.resolved(renderAlternatives(instruction, describers, references))
      is Instruction.Per -> Rendering.resolved(renderPer(instruction, describers, references))
      is Instruction.Gated -> Rendering.resolved(renderGated(instruction, describers, references))
      is Instruction.Then ->
          Rendering.resolved(
              renderCardRevealAndRestore(instruction, describers)
                  ?: renderCardPlaySequence(instruction, describers)
                  ?: renderStandardResourceCostSequence(instruction, describers, references)
                  ?: renderDiscardCostSequence(instruction, describers, references)
                  ?: renderCardResourceCostSequence(instruction, describers, references)
                  ?: renderSequentialThen(instruction, describers, references)
          )
      is NoOp -> Rendering.resolved(doNothingClause)
      is Instruction.Transform -> error("Transforms are expanded before ordinary instructions")
      is Instruction.By -> Rendering.resolved(null)
    }

private fun renderStandardResourceCostSequence(
    instruction: Instruction.Then,
    describers: Describers,
    references: TypeVariableReferences,
): Clause.Simple? {
  val removal = instruction.stages.singleOrNull() as? Remove ?: return null
  if (
      removal.intensity.modality() != Modality.REQUIRED ||
          !removal.removing.simple ||
          !describers.isStandardResource(removal.removing.className)
  ) {
    return null
  }
  val count = removal.count.fixedQuantity() ?: return null
  val result =
      renderLoweredInstructions(instruction.continuation, describers, references)
          .clauses
          .singleOrNull() ?: return null
  return Clause.Simple(
      Predicate(
          Verb("pay"),
          Coordination.one(describers.componentNounPhrase(removal.removing.className, count)),
          listOf(Modifier.Purpose(result)),
      )
  )
}

private fun renderDiscardCostSequence(
    instruction: Instruction.Then,
    describers: Describers,
    references: TypeVariableReferences,
): Clause.Simple? {
  val removal = instruction.stages.singleOrNull() as? Remove ?: return null
  val discarded =
      renderChange(removal, describers, references).value as? Clause.Simple ?: return null
  if (describers.changeFrame(removal.removing.className) !is ComponentDescriber.ChangeFrame.Deck) {
    return null
  }
  val result =
      renderLoweredInstructions(instruction.continuation, describers, references)
          .clauses
          .singleOrNull() ?: return null
  return discarded.withModifier(Modifier.Purpose(result))
}

private fun renderSequentialThen(
    instruction: Instruction.Then,
    describers: Describers,
    references: TypeVariableReferences,
): Clause? {
  val clauses =
      (instruction.stages + instruction.continuation).map { part ->
        renderLoweredInstructions(part, describers, references).clauses.singleOrNull()
            ?: return null
      }
  return Clause.Coordinated(Coordination(clauses, Conjunction.THEN))
}

private fun renderCardResourceCostSequence(
    instruction: Instruction.Then,
    describers: Describers,
    references: TypeVariableReferences,
): Clause.Simple? {
  val removal = instruction.stages.singleOrNull() as? Remove ?: return null
  val resolved = describers.resolveCardResource(removal.removing) ?: return null
  if (
      removal.intensity.modality() != Modality.REQUIRED ||
          !describers.cardResourceHasHolder(resolved, describers.thisExpression) ||
          removal.removing.refinement != null
  ) {
    return null
  }
  val count = removal.count.fixedQuantity() ?: return null
  val resource = describers.cardResourceNounPhrase(removal.removing.className, count) ?: return null
  val result =
      renderLoweredInstructions(instruction.continuation, describers, references)
          .clauses
          .singleOrNull() ?: return null
  return Clause.Simple(
      Predicate(
          Verb("remove"),
          Coordination.one(resource),
          listOf(
              Modifier.Phrase("from this card"),
              Modifier.Purpose(result),
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
          "reducing its cost by ${reduction.phrase.linearize()}"
        }
      }
  return Clause.Simple(
      Predicate(
          Verb("play"),
          Coordination.one(NounPhrase("card from hand", determiner = Determiner.INDEFINITE)),
          listOf(Modifier.Supplement(modifier)),
      )
  )
}

private fun renderGated(
    instruction: Instruction.Gated,
    describers: Describers,
    references: TypeVariableReferences,
): Clause? {
  val clause =
      renderLoweredInstructions(instruction.inner, describers, references).clauses.singleOrNull()
          ?: return null
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
  return Clause.Prefaced(Clause.Preface.Conditional(condition), clause)
}

private fun renderPer(
    instruction: Instruction.Per,
    describers: Describers,
    references: TypeVariableReferences,
): Clause? {
  val clause =
      renderLoweredInstructions(instruction.inner, describers, references).clauses.singleOrNull()
          ?: return null
  val metric =
      renderMetricPhrase(instruction.metric, describers)
          ?: (instruction.metric as? Metric.Count)?.expression?.let {
            renderCountedRelationToAntecedent(it, describers, references)
          }
          ?: return null
  return (clause as? Clause.Simple)?.withModifier(Modifier.Per(metric))
}

private fun renderAlternatives(
    instruction: Instruction.Or,
    describers: Describers,
    references: TypeVariableReferences,
): Clause? {
  renderPlacementSiteFallback(instruction, describers)?.let {
    return it
  }
  val alternatives =
      instruction.instructions.map { option ->
        renderLoweredInstructions(option, describers, references).clauses.singleOrNull()
            ?: return null
      }
  if (alternatives.size == 2) {
    val firstAction = alternatives.singleOrNull {
      it is Clause.Prefaced && it.preface == Clause.Preface.FirstAction
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

private fun Clause?.isDoNothing(): Boolean = this === doNothingClause

private val doNothingClause: Clause.Simple =
    Clause.Simple(Predicate(Verb("do"), Coordination.one(NounPhrase.text("nothing"))))

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
          preferred.gaining.className != unrestricted.gaining.className ||
          preferredPlacement.owner != null ||
          preferredPlacement.unknownDependencies.isNotEmpty() ||
          unrestrictedPlacement.owner != null ||
          unrestrictedPlacement.sites.isNotEmpty() ||
          unrestrictedPlacement.unknownDependencies.isNotEmpty() ||
          unrestricted.gaining.refinement != null ||
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

internal fun Describers.renderGateCondition(requirement: Requirement): Clause? {
  val counting = requirement as? Requirement.Counting ?: return null
  val metric = counting.metric as? Metric.Count ?: return null
  val expression = metric.expression
  val resolved = resolveExpression(expression) ?: return null
  if (resolved.sourceDependencies.isNotEmpty() || expression.refinement != null) {
    return null
  }
  if (
      requirement is Requirement.Exact &&
          requirement.expected == 1 &&
          isGameParticipant(expression.className)
  ) {
    return Clause.Simple(
        Predicate(
            Verb.BE,
            Coordination.one(NounPhrase("solo game", determiner = Determiner.INDEFINITE)),
        ),
        NounPhrase.text("this"),
    )
  }
  if (
      requirement is Requirement.Max &&
          requirement.maximum == 0 &&
          isStandardResource(expression.className)
  ) {
    return Clause.Simple(
        Predicate(
            Verb.HAVE,
            Coordination.one(
                componentNounPhrase(expression.className, 2)
                    .copy(
                        count = null,
                        determiner = Determiner.NO,
                        grammaticalNumber = NounPhrase.GrammaticalNumber.PLURAL,
                    )
            ),
        ),
        NounPhrase.you(),
    )
  }
  if (requirement !is Requirement.Min) return null
  fact(expression.className, ComponentDescriber::presenceCondition)?.let { condition ->
    if (requirement.minimum != 1) return null
    val predicate = Predicate(Verb(condition))
    return Clause.Simple(
        if (isGenerationScoped(expression.className)) {
          predicate.withModifier(Modifier.Phrase("this generation"))
        } else {
          predicate
        }
    )
  }
  fact(expression.className, ComponentDescriber::requirement)?.minimum?.let { bound ->
    if (bound is ComponentDescriber.Requirement.Bound.Count) {
      val amount = NounPhrase(bound.noun.singular, bound.noun.plural, count = requirement.minimum)
      return if (isPlayerOwned(expression.className)) {
        Clause.Simple(
            Predicate(
                Verb.HAVE,
                Coordination.one(amount),
            ),
            NounPhrase.you(),
        )
      } else {
        Clause.Simple(
            Predicate(
                Verb.BE,
                Coordination.one(amount),
            ),
            NounPhrase("there", grammaticalNumber = amount.number()),
        )
      }
    }
  }
  val name = tagName(expression.className) ?: return null
  return Clause.Simple(
      Predicate(
          Verb.HAVE,
          Coordination.one(NounPhrase("$name tag", "$name tags", count = requirement.minimum)),
      ),
      NounPhrase.you(),
  )
}
