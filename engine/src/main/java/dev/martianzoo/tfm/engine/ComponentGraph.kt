package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.ChangeRecord.StateChange
import dev.martianzoo.tfm.types.PType
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
  ): StateChange {
    // verify dependencies
    gaining?.let { g ->
      require(multiset.containsAll(g.dependencies)) {
        "New component $g is missing dependencies: " + (g.dependencies - multiset.elements)
      }
    }
    removing?.let { r ->
      multiset.elements.forEach {
        require(r !in it.dependencies) { "Can't remove dependency $r of existing component $it" }
      }
    }

    val correctedCount = updateMultiset(count, gaining, removing, amap)

    return StateChange(
        count = correctedCount,
        gaining = gaining?.type?.expression,
        removing = removing?.type?.expression,
    )
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

  fun hasType(cpt: Component, type: PType): Boolean {
    return cpt.alwaysHasType(type)
    // BIGTODO check refinements too
  }
}
