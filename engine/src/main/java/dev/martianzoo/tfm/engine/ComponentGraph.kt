package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.engine.Exceptions.DependencyException
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.MutableMultiset

/** All the components making up the state of a single [Game]. */
public class ComponentGraph {
  private val multiset: MutableMultiset<Component> = HashMultiset()

  public operator fun contains(component: Component) = component in multiset.elements

  public fun count(mtype: MType) = getAll(mtype).size

  public fun getAll(mtype: MType): Multiset<Component> = multiset.filter { it.hasType(mtype) }

  internal fun allActiveEffects(game: Game): Multiset<ActiveEffect> =
      multiset.flatMap { it.effects(game) }

  // only called by Game.silentChange
  internal fun update(
      count: Int = 1,
      gaining: Component? = null,
      removing: Component? = null,
      amap: Boolean = false,
  ): StateChange? {
    require(count >= 1) // TODO prevent this

    // TODO rethink how this whole method works

    checkDependencies(gaining, removing)

    val correctedCount = updateMultiset(count, gaining, removing, amap)
    return if (correctedCount == 0) {
      null
    } else {
      StateChange(
          count = correctedCount,
          gaining = gaining?.mtype?.expression,
          removing = removing?.mtype?.expression,
      )
    }
  }

  private fun checkDependencies(gaining: Component?, removing: Component?) {
    require(gaining != removing)

    // TODO might need to check amounts later

    if (gaining != null) {
      if (!multiset.containsAll(gaining.dependencyComponents)) {
        throw DependencyException.gaining(gaining.dependencyComponents - multiset.elements)
      }
    }

    if (removing != null) {
      val dependents = dependentsOf(removing)
      if (dependents.any()) {
        throw DependencyException.removing(dependents)
      }
    }
  }

  // Only called above and by rollback()
  internal fun updateMultiset(
      count: Int,
      gaining: Component? = null,
      removing: Component? = null,
      amap: Boolean = false,
  ): Int {
    require(gaining != removing)
    val correctedCount: Int =
        if (amap) {
          removing?.let { multiset.tryRemove(it, count) } ?: count
        } else {
          removing?.let { multiset.mustRemove(it, count) }
          count
        }
    gaining?.let { multiset.add(it, correctedCount) }
    return correctedCount
  }

  internal fun dependentsOf(dependency: Component) =
      multiset.filter { dependency in it.dependencyComponents }
}
