package dev.martianzoo.tfm.engine

import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.Multiset
import com.google.common.collect.Multisets
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.types.PType
import kotlin.math.min

public class ComponentGraph(startingWith: Collection<Component> = listOf()) {
  private val multiset: /*Mutable*/ Multiset<Component> = LinkedHashMultiset.create(startingWith)
  private val changeLog: MutableList<StateChange> = mutableListOf()

  public fun changeLog() = changeLog.toList()

  public fun applyChange(
      count: Int,
      gaining: Component? = null,
      removing: Component? = null,
      cause: Cause? = null,
      amap: Boolean = false,
  ) {
    require(gaining != removing)

    // TODO deal with limits

    val current = multiset.count(removing)
    val correctedCount =
        if (removing == null) {
          count // for now, we can just trust this works
        } else {
          if (amap) {
            min(count, current)
          } else {
            if (current < count) throw PetException("not enough")
            count
          }
        }

    removing?.let { multiset.remove(it, correctedCount) }
    gaining?.let { multiset.add(it, correctedCount) }

    // Creating this first should catch various errors
    val change =
        StateChange(
            count = correctedCount,
            gaining = gaining?.asTypeExpression?.asGeneric(),
            removing = removing?.asTypeExpression?.asGeneric(),
            cause = cause,
        )
    changeLog.add(change)
  }

  public fun count(type: PType) =
      multiset.entrySet().filter { it.element.hasType(type) }.sumOf { it.count }

  public fun getAll(type: PType): Multiset<Component> =
      Multisets.filter(multiset) { it!!.hasType(type) }

  public data class Component(val type: PType) {
    init {
      require(!type.abstract) { type }
    }

    public fun hasType(thatType: PType) = type.isSubtypeOf(thatType)
    public val asTypeExpression = type.toTypeExpression()
    override fun toString() = "[$type]"
  }
}
