package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset

interface ComponentGraph {
  operator fun contains(component: Component): Boolean
  fun count(parentType: MType): Int
  fun countComponent(component: Component): Int
  fun getAll(parentType: MType): Multiset<Component>
  fun activeEffects(game: Game): List<ActiveEffect>
}
