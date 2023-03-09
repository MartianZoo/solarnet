package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.ChangeEvent.StateChange
import dev.martianzoo.tfm.engine.Exceptions.DependencyException
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.MutableMultiset

/** All the components making up the state of a single [Game]. */
public class ComponentGraph {
  private val multiset: MutableMultiset<Component> = HashMultiset()

  public fun applyChange(
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

    if (gaining != null) {
      if (!multiset.containsAll(gaining.dependencyComponents)) {
        throw DependencyException.gaining(gaining, gaining.dependencyComponents - multiset.elements)
      }
    }

    if (removing != null) {
      val dependents = multiset.elements.filter { removing in it.dependencyComponents }
      if (dependents.any()) {
        throw DependencyException.removing(removing, dependents)
      }
    }
  }

  internal fun updateMultiset(
      count: Int,
      gaining: Component? = null,
      removing: Component? = null,
      amap: Boolean = false,
  ): Int {
    require(gaining != removing)

    // TODO deal with limits

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

  public fun count(mtype: MType) = getAll(mtype).size

  // Aww yeah full table scans rule. One day I'll do something more clever, but only after being
  // able to review usage patterns so I'll actually know what helps most.
  public fun getAll(mtype: MType): Multiset<Component> = multiset.filter { hasType(it, mtype) }

  public operator fun contains(component: Component) = component in multiset.elements

  fun hasType(cpt: Component, type: MType): Boolean {
    return cpt.hasType(type)
    // BIGTODO check refinements too
  }

  internal fun allActiveEffects(): Multiset<ActiveEffect> = multiset.flatMap { it.activeEffects }
}
