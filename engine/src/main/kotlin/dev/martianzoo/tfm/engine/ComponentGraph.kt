package dev.martianzoo.tfm.engine

import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.Multiset
import com.google.common.collect.Multisets
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.types.PetType

public class ComponentGraph(startingWith: Collection<Component> = listOf()) {
  private val multiset: /*Mutable*/ Multiset<Component> = LinkedHashMultiset.create(startingWith)
  internal val changeLog: MutableList<StateChange> = mutableListOf() // starts with ordinal 1

  public fun gain(count: Int, gaining: Component, cause: Cause? = null) =
      applyChange(count, gaining = gaining, cause = cause)

  public fun remove(count: Int, removing: Component, cause: Cause? = null) =
      applyChange(count, removing = removing, cause = cause)

  public fun transmute(count: Int, gaining: Component, removing: Component, cause: Cause? = null) =
      applyChange(count, gaining, removing, cause)

  public fun applyChange(
      count: Int,
      gaining: Component? = null,
      removing: Component? = null,
      cause: Cause? = null,
  ) {

    // Creating this first should catch various errors
    val change =
        StateChange(
            ordinal = changeLog.size + 1,
            count = count,
            gaining = gaining?.asTypeExpression?.asGeneric(),
            removing = removing?.asTypeExpression?.asGeneric(),
            cause = cause,
        )

    removing?.let { multiset.mustRemove(it, count) }
    gaining?.let { multiset.add(it, count) }
    changeLog.add(change)
  }

  // this is how to make sure the whole remove happens?
  // who designed this crap?
  private fun <E : Any?> Multiset<E>.mustRemove(element: E, count: Int) =
      setCount(element, count(element) - count)

  public fun count(type: PetType) =
      multiset.entrySet().filter { it.element.hasType(type) }.sumOf { it.count }

  public fun getAll(type: PetType): Multiset<Component> =
      Multisets.filter(multiset) { it!!.hasType(type) }

  public data class Component(val type: PetType) {
    init {
      require(!type.abstract) { type }
    }

    public fun hasType(thatType: PetType) = type.isSubtypeOf(thatType)
    public val asTypeExpression = type.toTypeExpression()
    override fun toString() = "[$type]"
  }
}
