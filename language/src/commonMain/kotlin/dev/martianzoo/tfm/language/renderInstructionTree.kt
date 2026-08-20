package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar

internal fun renderInstructionTree(
    instructionTree: InstructionTree,
    describers: Describers,
): String? = renderInstructions(instructionTree, describers)?.asSentences()

internal fun renderInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
): RenderedInstructions? =
    renderLoweredInstructions(lowerProductionSyntax(instructionTree), describers)

private fun renderLoweredInstructions(
    instructionTree: InstructionTree,
    describers: Describers,
): RenderedInstructions? {
  val instructions = InstructionGroup.of(instructionTree).instructions
  if (instructions.isEmpty()) {
    return RenderedInstructions(
        listOf(Clause.Simple(Predicate("do", Coordination.one(NounPhrase.text("nothing")))))
    )
  }
  val rendered = instructions.map { instruction ->
    instruction to (renderInstruction(instruction, describers) ?: return null)
  }
  return RenderedInstructions(coalesceAdjacentChanges(rendered, describers))
}

private fun renderInstruction(
    instruction: Instruction,
    describers: Describers,
): Clause? =
    when (instruction) {
      is Gain,
      is Remove,
      is Instruction.Transmute -> renderChange(instruction, describers)
      is Instruction.Or -> renderAlternatives(instruction, describers)
      is Instruction.Per -> renderPer(instruction, describers)
      is Instruction.Gated -> renderGated(instruction, describers)
      is Instruction.Then -> renderCardPlaySequence(instruction, describers)
      is NoOp,
      is Instruction.By,
      is Instruction.Transform -> null
    }

private fun renderCardPlaySequence(
    instruction: Instruction.Then,
    describers: Describers,
): Clause.Simple? {
  val play = instruction.stages.singleOrNull() as? Gain ?: return null
  if (
      (play.intensity != null && play.intensity != MANDATORY) ||
          !play.gaining.simple ||
          (play.count as? ActualScalar)?.value != 1 ||
          describers.fact(play.gaining.className, ComponentDescriber::playTrigger) !=
              ComponentDescriber.PlayTrigger.CARD
  ) {
    return null
  }
  val modifier =
      when (val continuation = instruction.continuation) {
        is Instruction.Per -> {
          val removal = continuation.inner as? Remove ?: return null
          val counted = continuation.metric as? Metric.Count ?: return null
          if (
              removal.intensity != MANDATORY ||
                  !removal.removing.simple ||
                  removal.removing != counted.expression ||
                  (removal.count as? ActualScalar)?.value != 1 ||
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
      renderLoweredInstructions(instruction.inner, describers)?.clauses?.singleOrNull()
          ?: return null
  val condition = describers.renderGateCondition(instruction.gate) ?: return null
  return (clause as? Clause.Simple)?.withModifier(Modifier.Phrase(condition))
}

private fun renderPer(
    instruction: Instruction.Per,
    describers: Describers,
): Clause? {
  val clause =
      renderLoweredInstructions(instruction.inner, describers)?.clauses?.singleOrNull()
          ?: return null
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
        renderLoweredInstructions(option, describers)?.clauses?.singleOrNull() ?: return null
      }
  val simpleAlternatives = alternatives.map { it as? Clause.Simple }
  if (simpleAlternatives.all { it != null }) {
    coordinateClauseObjects(simpleAlternatives.filterNotNull(), Conjunction.OR)?.let {
      return it
    }
  }
  return Clause.Coordinated(Coordination(alternatives, Conjunction.OR))
}

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
  if (
      preferred.gaining.refinement != null ||
          preferred.gaining.complement ||
          preferred.gaining.className != unrestricted.gaining.className ||
          unrestricted.gaining.arguments.isNotEmpty() ||
          unrestricted.gaining.refinement != null ||
          unrestricted.gaining.complement ||
          preferred.intensity != unrestricted.intensity ||
          preferred.count != unrestricted.count ||
          (preferred.count as? ActualScalar)?.value != 1
  ) {
    return null
  }
  val site = preferred.gaining.arguments.singleOrNull()?.takeIf { it.simple } ?: return null
  if (describers.placementSite(site.className) == null) return null
  val placement =
      describers.fact(preferred.gaining.className, ComponentDescriber::placement) ?: return null
  if (placement.consequence != null) return null
  val absence = fallback.gate as? Requirement.Max ?: return null
  val countedSite = absence.countedMetric as? Metric.Count ?: return null
  if (absence.maximum != 0 || countedSite.expression != site) return null

  val preferredClause = renderChange(preferred, describers) as? Clause.Simple ?: return null
  if (renderChange(unrestricted, describers) !is Clause.Simple) return null
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

private fun Describers.renderGateCondition(requirement: Requirement): String? {
  val minimum = requirement as? Requirement.Min ?: return null
  val metric = minimum.metric as? Metric.Count ?: return null
  if (!metric.expression.simple) return null
  val (name) = tagName(metric.expression.className) ?: return null
  val tags = if (minimum.target == 1) "tag" else "tags"
  return "if you have ${minimum.target} $name $tags"
}
