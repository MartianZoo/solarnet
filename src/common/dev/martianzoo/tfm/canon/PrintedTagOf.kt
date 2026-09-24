package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.types.Type

/** Reads immutable printed tags from one represented card front Class. */
internal object PrintedTagOf : CustomMetric() {
  override fun count(game: GameReader, type: Type): Int {
    val (back, tagClass) = type.typeDependencies.map { it.boundType }
    val front = back.typeDependencies.mapNotNull { it.boundType.representedClass }.single()
    val tag = requireNotNull(tagClass.representedClass)
    return cardTags(game.tfmCatalog.card(front.className)).count(tag.className)
  }
}
