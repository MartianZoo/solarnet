package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.types.Type

internal val milestonesAwardsExpansionBundle: StandardFormBundle =
    StandardFormBundle(
        "MilestonesAwardsExpansion",
        setOf(MilestonesAwardsExpansion.GainsOf),
    )

private object MilestonesAwardsExpansion {
  object GainsOf : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val (cardExpression, targetExpression) = type.expressionFull.arguments
      if (game.countComponent(game.resolve(cardExpression)) == 0) return 0
      val effects = cardEffects(game.tfmCatalog.card(cardExpression.className))
      val target = targetExpression.arguments.single().className
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
