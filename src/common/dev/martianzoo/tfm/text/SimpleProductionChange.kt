package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Transmute

/** A fixed mandatory change to one concrete production track. */
internal data class SimpleProductionChange(
    val gaining: Boolean,
    val resource: ClassName,
    val owner: Expression?,
    val count: Int,
)

internal fun simpleProductionChange(
    instruction: Instruction,
    describers: Describers,
): SimpleProductionChange? {
  val change = instruction as? Instruction.Change ?: return null
  if (change is Transmute || change.quantifier.modality() != Modality.REQUIRED) return null
  val gaining =
      when (change) {
        is Gain -> true
        is Remove -> false
        is Transmute -> return null
      }
  val expression = change.gaining ?: change.removing ?: return null
  val production = productionExpression(expression, describers) ?: return null
  val count = change.count.fixedQuantity() ?: return null
  return SimpleProductionChange(gaining, production.resource, production.owner, count)
}
