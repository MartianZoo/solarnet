package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.CustomMetric
import dev.martianzoo.state.GameReader

/** Counts gain instructions authored by a card class, including subtypes of the target class. */
internal object GainsOf : CustomMetric() {
  override fun count(game: GameReader, type: Type): Int {
    val (subject, target) = type.typeDependencies.map { it.boundType }
    val definition = requireNotNull(subject.representedClass).declaration
    val targetClass = requireNotNull(target.representedClass)
    return definition.authoredEffects.sumOf { effect ->
      effect.instruction.descendantsOfType<Instruction.Change>().count { change ->
        val gained = change.gaining ?: return@count false
        game.classTable.getClass(gained.className).isSubtypeOf(targetClass)
      }
    }
  }
}
