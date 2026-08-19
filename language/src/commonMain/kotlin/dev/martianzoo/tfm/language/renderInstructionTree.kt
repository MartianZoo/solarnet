package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.TfmClasses.PROD

internal fun renderInstructionTree(
    instructionTree: InstructionTree,
    card: CardDefinition? = null,
): String? =
    renderInstructionClauses(instructionTree, card)?.joinToString(" ") { completeSentence(it) }

internal fun renderInstructionClauses(
    instructionTree: InstructionTree,
    card: CardDefinition? = null,
): List<String>? {
  val instructions = InstructionGroup.of(instructionTree).instructions
  if (instructions.isEmpty()) return listOf("do nothing")
  val clauses = mutableListOf<String>()
  var index = 0
  while (index < instructions.size) {
    if (standardResourceGain(instructions[index]) != null) {
      val gains = instructions.drop(index).takeWhile { standardResourceGain(it) != null }
      clauses += renderStandardResourceGains(gains)
      index += gains.size
    } else {
      clauses +=
          renderStandardResourceRemoval(instructions[index])
              ?: renderDirectGain(instructions[index])
              ?: renderCardResourceGain(instructions[index], card)
              ?: renderProductionChange(instructions[index])
              ?: renderTrackChange(instructions[index])
              ?: renderTilePlacement(instructions[index])
              ?: return null
      index++
    }
  }
  return clauses
}

private fun renderStandardResourceGains(instructions: List<Instruction>): String {
  val objects = instructions.map { instruction ->
    val (className, count) = checkNotNull(standardResourceGain(instruction))
    "$count ${componentNoun(className, count)}"
  }
  return "gain ${englishList(objects)}"
}

private fun renderStandardResourceRemoval(instruction: Instruction): String? {
  standardResourceRemoval(instruction)?.let { (className, count) ->
    return "remove $count ${componentNoun(className, count)}"
  }
  val removal = standardResourceRemovalFromAnyPlayer(instruction) ?: return null
  if (removal.intensity != OPTIONAL) return null
  return "remove up to ${removal.count} ${componentNoun(removal.className, removal.count)} from any player"
}

private fun renderDirectGain(instruction: Instruction): String? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  val gain = Describers[className].directGain ?: return null
  if (count != gain.count) return null
  return "gain $count ${gain.noun}"
}

private fun renderCardResourceGain(
    instruction: Instruction,
    card: CardDefinition?,
): String? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity != null && gain.intensity != MANDATORY) return null
  val expression = gain.gaining
  if (expression.refinement != null || expression.complement) return null
  val count = (gain.count as? ActualScalar)?.value ?: return null
  val noun = cardResourceNoun(expression.className, count) ?: return null
  val target =
      when {
        expression.arguments == listOf(thisExpression) -> "this card"
        expression.arguments.isNotEmpty() -> return null
        card == null -> "an eligible card"
        card.resourceType == expression.className -> "ANY card"
        else -> "ANOTHER card"
      }
  return "add $count $noun to $target"
}

private fun renderProductionChange(instruction: Instruction): String? {
  val transform = instruction as? Transform ?: return null
  if (transform.transformKind != PROD) return null
  val instructions = InstructionGroup.of(transform.instruction).instructions
  val changes = instructions.map { productionChange(it) ?: return null }
  val clauses = mutableListOf<String>()
  var index = 0
  while (index < changes.size) {
    val run = changes.drop(index).takeWhile { it.gaining == changes[index].gaining }
    clauses += renderProductionClause(run)
    index += run.size
  }
  return clauses.joinToString(" and ")
}

private fun productionChange(instruction: Instruction): ResourceProductionChange? {
  standardResourceGain(instruction)?.let { (className, count) ->
    return ResourceProductionChange(true, "your", className, count)
  }
  standardResourceRemoval(instruction)?.let { (className, count) ->
    return ResourceProductionChange(false, "your", className, count)
  }
  standardResourceRemovalFromAnyPlayer(instruction)?.let { removal ->
    if (removal.intensity != null && removal.intensity != MANDATORY) return null
    return ResourceProductionChange(false, "any player's", removal.className, removal.count)
  }
  return null
}

private fun renderProductionClause(changes: List<ResourceProductionChange>): String {
  val verb = if (changes.first().gaining) "increase" else "decrease"
  val productions = changes.map {
    val steps = if (it.count == 1) "step" else "steps"
    "${it.owner} ${componentNoun(it.className, 1)} production ${it.count} $steps"
  }
  return "$verb ${englishList(productions)}"
}

private fun standardResourceGain(instruction: Instruction): Pair<ClassName, Int>? {
  val gain = concreteMandatoryGain(instruction) ?: return null
  return gain.takeIf { (className) -> isStandardResource(className) }
}

private fun renderTrackChange(instruction: Instruction): String? {
  val gain = concreteMandatoryGain(instruction)
  val removal = concreteMandatoryRemoval(instruction)
  val (className, count) = gain ?: removal ?: return null
  val track = Describers[className].track ?: return null
  val steps = if (count == 1) "step" else "steps"
  val verb = if (gain != null) "raise" else "lower"
  return "$verb ${track.subject} $count $steps"
}

private fun renderTilePlacement(instruction: Instruction): String? {
  val (className, count) = concreteMandatoryGain(instruction) ?: return null
  val placement = Describers[className].placement ?: return null
  if (count != 1 && !placement.allowsMultiple) return null
  val nounPhrase =
      if (count == 1) {
        "${placement.article} ${placement.singular}"
      } else {
        "$count ${placement.plural}"
      }
  val consequence = placement.consequence?.let { " ($it)" }.orEmpty()
  return "place $nounPhrase$consequence"
}

private fun concreteMandatoryGain(instruction: Instruction): Pair<ClassName, Int>? {
  val gain = instruction as? Gain ?: return null
  if (gain.intensity != null && gain.intensity != MANDATORY) return null
  if (!gain.gaining.simple) return null
  val count = (gain.count as? ActualScalar)?.value ?: return null
  return gain.gaining.className to count
}

private fun standardResourceRemoval(instruction: Instruction): Pair<ClassName, Int>? {
  val removal = concreteMandatoryRemoval(instruction) ?: return null
  return removal.takeIf { (className) -> isStandardResource(className) }
}

private fun standardResourceRemovalFromAnyPlayer(
    instruction: Instruction
): TargetedResourceRemoval? {
  val removal = instruction as? Remove ?: return null
  val expression = removal.removing
  if (expression.complement || expression.refinement != null) return null
  if (expression.arguments != listOf(anyoneExpression)) return null
  if (!isStandardResource(expression.className)) return null
  val count = (removal.count as? ActualScalar)?.value ?: return null
  return TargetedResourceRemoval(expression.className, count, removal.intensity)
}

private fun concreteMandatoryRemoval(instruction: Instruction): Pair<ClassName, Int>? {
  val removal = instruction as? Remove ?: return null
  if (removal.intensity != null && removal.intensity != MANDATORY) return null
  if (!removal.removing.simple) return null
  val count = (removal.count as? ActualScalar)?.value ?: return null
  return removal.removing.className to count
}

private fun englishList(parts: List<String>): String =
    when (parts.size) {
      1 -> parts.single()
      2 -> parts.joinToString(" and ")
      else -> parts.dropLast(1).joinToString(", ") + ", and " + parts.last()
    }

private fun completeSentence(clause: String): String =
    clause.replaceFirstChar(Char::uppercaseChar) + "."

private val anyoneExpression = cn("Anyone").expression
private val thisExpression = cn("This").expression

private data class ResourceProductionChange(
    val gaining: Boolean,
    val owner: String,
    val className: ClassName,
    val count: Int,
)

private data class TargetedResourceRemoval(
    val className: ClassName,
    val count: Int,
    val intensity: Instruction.Intensity?,
)
