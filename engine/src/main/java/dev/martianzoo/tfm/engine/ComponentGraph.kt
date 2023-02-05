package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.ChangeLogEntry
import dev.martianzoo.tfm.data.ChangeLogEntry.Cause
import dev.martianzoo.tfm.data.ChangeLogEntry.StateChange
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.MutableMultiset
import dev.martianzoo.util.filter

/** All the components making up the state of a single [Game]. */
public class ComponentGraph(startingWith: Collection<Component> = listOf()) {
  private val multiset: MutableMultiset<Component> = HashMultiset()
  private val changeLog: MutableList<ChangeLogEntry> = mutableListOf()

  init {
    multiset.addAll(startingWith)
  }

  public fun changeLog() = changeLog.toList()

  public fun applyChange(
      count: Int = 1,
      gaining: Component? = null,
      removing: Component? = null,
      cause: Cause? = null,
      amap: Boolean = false,
      hidden: Boolean = false
  ) {
    val correctedCount = updateMultiset(count, gaining, removing, amap)
    changeLog.add(
        ChangeLogEntry(
            ordinal = changeLog.size,
            change = StateChange(
                count = correctedCount,
                gaining = gaining?.asTypeExpr,
                removing = removing?.asTypeExpr,
            ),
            cause = cause,
            hidden = hidden,
        )
    )
  }

  private fun updateMultiset(
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
  public fun getAll(ptype: PType): Multiset<Component> = multiset.filter { it.hasType(ptype) }
}
