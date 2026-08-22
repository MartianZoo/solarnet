package dev.martianzoo.data

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement

/** Constructive Module gains caused by creating the source Module itself. */
public object ModuleProvenance {
  /** One gained Class and the configuration requirements guarding that gain. */
  public data class ProvenanceGain(
      public val target: ClassName,
      public val requirements: List<Requirement>,
  )

  /** Extracts simple gains reached directly from the declaration's own creation. */
  public fun gains(declaration: ClassDeclaration): List<ProvenanceGain> = buildList {
    fun triggerRequirements(trigger: Trigger): List<Requirement>? =
        when (trigger) {
          WhenGain -> emptyList()
          is IfTrigger ->
              triggerRequirements(trigger.inner)?.let { requirements ->
                requirements + trigger.condition
              }
          else -> null
        }

    fun collect(tree: InstructionTree, requirements: List<Requirement>) {
      when (tree) {
        is InstructionGroup -> tree.instructions.forEach { collect(it, requirements) }
        is Gated -> collect(tree.inner, requirements + tree.gate)
        is Gain ->
            tree.gaining
                .takeIf { it.simple }
                ?.let { gaining ->
                  add(ProvenanceGain(gaining.className, requirements))
                }
        else -> Unit
      }
    }

    declaration.effects.forEach { effect ->
      triggerRequirements(effect.trigger)?.let { requirements ->
        collect(effect.instruction, requirements)
      }
    }
  }
}
