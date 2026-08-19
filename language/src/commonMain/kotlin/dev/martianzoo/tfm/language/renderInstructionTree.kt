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
): String? = renderInstructions(instructionTree, card)?.asSentences()

internal fun renderInstructions(
    instructionTree: InstructionTree,
    card: CardDefinition? = null,
): RenderedInstructions? = renderLoweredInstructions(lowerProductionSyntax(instructionTree), card)

private fun renderLoweredInstructions(
    instructionTree: InstructionTree,
    card: CardDefinition?,
): RenderedInstructions? {
  val instructions = InstructionGroup.of(instructionTree).instructions
  if (instructions.isEmpty()) return RenderedInstructions(listOf("do nothing"))
  val clauses = mutableListOf<String>()
  var index = 0
  while (index < instructions.size) {
    val run = Describers.renderInstructionRun(instructions.drop(index))
    if (run != null) {
      clauses += run.clause
      index += run.count
      continue
    }
    clauses += renderInstruction(instructions[index], card) ?: return null
    index++
  }
  return RenderedInstructions(clauses)
}

private fun renderInstruction(instruction: Instruction, card: CardDefinition?): String? =
    when (instruction) {
      is Gain,
      is Remove -> Describers.renderChange(instruction, card)
      is Instruction.Or -> renderAlternatives(instruction, card)
      is Instruction.Per -> renderPer(instruction, card)
      is Instruction.Gated -> renderGated(instruction, card)
      is NoOp,
      is Instruction.By,
      is Instruction.Then,
      is Instruction.Transform,
      is Instruction.Transmute -> null
    }

private fun renderGated(instruction: Instruction.Gated, card: CardDefinition?): String? {
  val clause =
      renderLoweredInstructions(instruction.inner, card)?.clauses?.singleOrNull() ?: return null
  val condition = Describers.renderGateCondition(instruction.gate) ?: return null
  return "$clause $condition"
}

private fun renderPer(instruction: Instruction.Per, card: CardDefinition?): String? {
  val clause =
      renderLoweredInstructions(instruction.inner, card)?.clauses?.singleOrNull() ?: return null
  val metric = renderMetricPhrase(instruction.metric) ?: return null
  return "$clause for $metric"
}

private fun renderAlternatives(
    instruction: Instruction.Or,
    card: CardDefinition?,
): String? {
  val alternatives =
      instruction.instructions.map { option ->
        renderLoweredInstructions(option, card)?.clauses?.singleOrNull() ?: return null
      }
  return englishAlternatives(alternatives)
}
