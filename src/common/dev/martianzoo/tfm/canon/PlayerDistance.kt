package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.CustomMetric
import dev.martianzoo.state.GameReader

/** Clockwise seat distance, with owners outside the player ring after every seated player. */
internal object PlayerDistance : CustomMetric() {
  private val AFTER_ME = cn("AfterMe")

  override fun count(game: GameReader, type: Type): Int {
    val (source, target) = type.typeDependencies.map { it.boundType }
    val relations = game.getComponents(game.resolve(AFTER_ME.expression))
    val successors =
        relations.elements.associate { relation ->
          val (before, after) = relation.typeDependencies.map { it.boundType }
          before to after
        }
    require(successors.size == relations.size) { "AfterMe must have one successor per Player" }

    if (target !in successors) return successors.size
    if (source !in successors) return 0
    if (source == target) return 0
    var current = source
    for (distance in 1..successors.size) {
      current = requireNotNull(successors[current]) { "$current has no AfterMe successor" }
      if (current == target) return distance
    }
    error("$target is not reachable from $source through AfterMe")
  }
}
