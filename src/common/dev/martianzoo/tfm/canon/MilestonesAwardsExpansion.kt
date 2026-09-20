package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.types.Type

internal val milestonesAwardsCustomClasses: Set<CustomMetric> =
    setOf(MilestonesAwardsExpansion.GainsOf)

private object MilestonesAwardsExpansion {
  object GainsOf : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val (cardType, targetClassType) = type.typeDependencies.map { it.boundType }
      val effects = cardEffects(game.tfmCatalog.card(cardType.className))
      val target = requireNotNull(targetClassType.representedClass).className
      return effects.sumOf { effect ->
        var gains = 0
        effect.visitDescendants { node ->
          if (node !is Instruction.Change) return@visitDescendants true
          gains +=
              node.gaining?.descendantsOfType<Expression>()?.count {
                it.className == target
              } ?: 0
          false
        }
        gains
      }
    }
  }
}
