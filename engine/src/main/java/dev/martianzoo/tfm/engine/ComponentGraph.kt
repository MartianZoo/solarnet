package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.ChangeRecord.StateChange
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.MutableMultiset

/** All the components making up the state of a single [Game]. */
public class ComponentGraph {
  private val multiset: MutableMultiset<Component> = HashMultiset()

  class MissingDependenciesException(message: String) : RuntimeException(message)

  public fun applyChange(
      count: Int = 1,
      gaining: Component? = null,
      removing: Component? = null,
      amap: Boolean = false,
  ): StateChange? {
    require(count >= 1)

    // TODO rethink how this whole method works

    // verify dependencies
    gaining?.let { g ->
      if (!multiset.containsAll(g.dependencyComponents)) {
        throw MissingDependenciesException(
            "Can't create $g due to missing dependencies: " +
                (g.dependencyComponents - multiset.elements))
      }
    }
    removing?.let { r ->
      multiset.elements.forEach {
        require(r !in it.dependencyComponents) {
          "Can't remove dependency $r of existing component $it"
        }
      }
    }

    val correctedCount = updateMultiset(count, gaining, removing, amap)
    return if (correctedCount == 0) {
      null
    } else {
      StateChange(
          count = correctedCount,
          gaining = gaining?.ptype?.expression,
          removing = removing?.ptype?.expression,
      )
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

  public fun count(ptype: PType) = getAll(ptype).size

  // Aww yeah full table scans rule. One day I'll do something more clever, but only after being
  // able to review usage patterns so I'll actually know what helps most.
  public fun getAll(ptype: PType): Multiset<Component> = multiset.filter { hasType(it, ptype) }

  public operator fun contains(component: Component) = component in multiset.elements

  fun hasType(cpt: Component, type: PType): Boolean {
    return cpt.hasType(type)
    // BIGTODO check refinements too
  }
}
