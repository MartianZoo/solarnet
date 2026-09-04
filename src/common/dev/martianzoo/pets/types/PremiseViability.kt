package dev.martianzoo.pets.types

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Or as InstructionOr
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue

/** Exact premise checks whose proofs depend only on uninhabited Types. */
internal object PremiseViability {
  fun validate(table: ClassTable, selectedClassNames: Set<ClassName>) {
    val interpreter =
        InhabitanceInterpreter(classIsUninhabited = { className -> !table.isActive(className) })
    selectedClassNames.forEach { className ->
      val declaration = table.getClass(className).declaration
      (declaration.properties[REQUIREMENT_PROPERTY] as? RequirementValue)?.let { property ->
        if (interpreter.requirementIsFalse(property.value)) {
          unviable(className, "impossible requirement ${property.value}")
        }
      }
      declaration.effects
          .filter { interpreter.triggerIsReachable(it.trigger) }
          .forEach { effect ->
            impossibleRemoval(effect.instruction, table, interpreter)?.let { removal ->
              unviable(className, "reachable mandatory removal $removal")
            }
          }
    }
  }

  private fun unviable(className: ClassName, reason: String): Nothing =
      throw IllegalArgumentException("unviable game premise: $className has $reason")

  private val REQUIREMENT_PROPERTY = PropertyName("requirement")

  private fun impossibleRemoval(
      tree: InstructionTree,
      table: ClassTable,
      interpreter: InhabitanceInterpreter,
  ): Expression? =
      when (tree) {
        is Gated ->
            if (interpreter.requirementIsFalse(tree.gate)) null
            else impossibleRemoval(tree.inner, table, interpreter)
        is InstructionOr ->
            tree.instructions
                .map { impossibleRemoval(it, table, interpreter) }
                .takeIf { results -> results.all { it != null } }
                ?.first()
        is Per ->
            if (interpreter.metricIsExactlyZero(tree.metric)) null
            else impossibleRemoval(tree.inner, table, interpreter)
        is Change ->
            tree.removing?.takeIf {
              interpreter.expressionIsUninhabited(it) &&
                  (tree.intensity ?: table.getClass(it.className).defaults.removeOnly.intensity) ==
                      MANDATORY
            }
        else ->
            tree.immediateChildren().filterIsInstance<InstructionTree>().firstNotNullOfOrNull {
              impossibleRemoval(it, table, interpreter)
            }
      }
}
