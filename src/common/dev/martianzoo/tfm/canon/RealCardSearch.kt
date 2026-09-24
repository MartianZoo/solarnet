package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression.Refinement.Has
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.types.Class

/** A filtered search consumes skipped cards without creating backs for them. */
internal object RealCardSearch : CustomClass("SearchForCard") {
  override fun translateGain(game: GameReader, gain: Gain): InstructionTree {
    val filter =
        (gain.gaining.refinement as? Has)?.requirement
            ?: throw ExpressionException("SearchForCard requires a printed-card predicate")
    return RealCardDraw.dealMatching(
        game,
        game.classTable.getClass(cn("ProjectCard")),
        cn("Hand").expression,
    ) { card ->
      filter.isMetBy { printedCount(card, it) }
    }
  }

  private fun printedCount(card: Class, metric: Metric): Int {
    val counted =
        (metric as? Metric.Count)?.expression
            ?: throw ExpressionException("Unsupported card-search metric: $metric")
    val tags = cardTags(card)
    return when (counted.className) {
      cn("PrintedTag") ->
          counted.arguments.singleOrNull()?.arguments?.singleOrNull()?.className?.let(tags::count)
              ?: tags.size
      cn("ReferenceTo") -> {
        val target =
            counted.arguments.singleOrNull()?.arguments?.singleOrNull()?.className
                ?: throw ExpressionException("ReferenceTo requires a Class argument")
        card.declaration.allNodes.count { node ->
          node.descendantsOfType<dev.martianzoo.pets.ast.ClassName>().any { it == target }
        }
      }
      else -> tags.count(counted.className)
    }
  }
}
