package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Type

internal val turmoilExpansionCustomClasses: Set<CustomMetric> =
    setOf(PartyDistance, PartyRequirement)

/** Computes forward distance through the declared Party ring. */
internal object PartyDistance : CustomMetric() {
  private val AFTER_PARTY = cn("AfterParty")

  override val requiredClassNames: Set<ClassName> = setOf(AFTER_PARTY)

  override fun count(game: GameReader, type: Type): Int {
    val (source, target) = type.typeDependencies.map { it.boundType }
    if (source == target) return 0

    val relations = game.getComponents(game.resolve(AFTER_PARTY.expression))
    val successors =
        relations.elements.associate { relation ->
          val (before, after) = relation.typeDependencies.map { it.boundType }
          before to after
        }
    require(successors.size == relations.size) { "AfterParty must have one successor per Party" }

    var current = source
    for (distance in 1..successors.size) {
      current = requireNotNull(successors[current]) { "$current has no AfterParty successor" }
      if (current == target) return distance
    }
    error("$target is not reachable from $source through AfterParty")
  }
}
