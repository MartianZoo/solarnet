package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.data.CardDefinition

internal fun renderInstructionTree(
    instructionTree: InstructionTree,
    card: CardDefinition? = null,
    describers: Describers,
): String? = renderInstructions(instructionTree, card, describers)?.asSentences()

internal fun renderInstructions(
    instructionTree: InstructionTree,
    card: CardDefinition? = null,
    describers: Describers,
): RenderedInstructions? =
    renderLoweredInstructions(lowerProductionSyntax(instructionTree), card, describers)

private fun renderLoweredInstructions(
    instructionTree: InstructionTree,
    card: CardDefinition?,
    describers: Describers,
): RenderedInstructions? {
  val instructions = InstructionGroup.of(instructionTree).instructions
  if (instructions.isEmpty()) return RenderedInstructions(listOf("do nothing"))
  val clauses = mutableListOf<String>()
  var index = 0
  while (index < instructions.size) {
    val run = describers.renderInstructionRun(instructions.drop(index))
    if (run != null) {
      clauses += run.clause
      index += run.count
      continue
    }
    clauses += renderInstruction(instructions[index], card, describers) ?: return null
    index++
  }
  return RenderedInstructions(clauses)
}

private fun renderInstruction(
    instruction: Instruction,
    card: CardDefinition?,
    describers: Describers,
): String? =
    when (instruction) {
      is Gain,
      is Remove -> describers.renderChange(instruction, card)
      is Instruction.Or -> renderAlternatives(instruction, card, describers)
      is Instruction.Per -> renderPer(instruction, card, describers)
      is Instruction.Gated -> renderGated(instruction, card, describers)
      is NoOp,
      is Instruction.By,
      is Instruction.Then,
      is Instruction.Transform,
      is Instruction.Transmute -> null
    }

private fun renderGated(
    instruction: Instruction.Gated,
    card: CardDefinition?,
    describers: Describers,
): String? {
  val clause =
      renderLoweredInstructions(instruction.inner, card, describers)?.clauses?.singleOrNull()
          ?: return null
  val condition = describers.renderGateCondition(instruction.gate) ?: return null
  return "$clause $condition"
}

private fun renderPer(
    instruction: Instruction.Per,
    card: CardDefinition?,
    describers: Describers,
): String? {
  val clause =
      renderLoweredInstructions(instruction.inner, card, describers)?.clauses?.singleOrNull()
          ?: return null
  val metric = renderMetricPhrase(instruction.metric, describers) ?: return null
  return "$clause for $metric"
}

private fun renderAlternatives(
    instruction: Instruction.Or,
    card: CardDefinition?,
    describers: Describers,
): String? {
  val alternatives =
      instruction.instructions.map { option ->
        renderLoweredInstructions(option, card, describers)?.clauses?.singleOrNull() ?: return null
      }
  return englishAlternatives(alternatives)
}
