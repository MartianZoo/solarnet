package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement

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
      is NoOp,
      is Instruction.By,
      is Instruction.Then,
      is Instruction.Transform -> null
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
  val alternatives =
      instruction.instructions.map { option ->
        renderLoweredInstructions(option, describers)?.clauses?.singleOrNull() ?: return null
      }
  val predicates = alternatives.map { (it as? Clause.Simple)?.predicate }
  if (predicates.all { it != null }) {
    val present = predicates.filterNotNull()
    val first = present.first()
    if (present.all { it.verb == first.verb && it.modifiers == first.modifiers }) {
      return Clause.Simple(
          first.copy(
              objects =
                  Coordination(
                      present.flatMap { it.objects.members },
                      Conjunction.OR,
                  )
          )
      )
    }
  }
  return Clause.Coordinated(Coordination(alternatives, Conjunction.OR))
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
    if (
        previous != null &&
            current != null &&
            previous.predicate.verb == current.predicate.verb &&
            previous.predicate.modifiers == current.predicate.modifiers
    ) {
      result[result.lastIndex] =
          Clause.Simple(
              previous.predicate.copy(
                  objects =
                      Coordination(
                          previous.predicate.objects.members + current.predicate.objects.members,
                          Conjunction.AND,
                      )
              )
          )
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
