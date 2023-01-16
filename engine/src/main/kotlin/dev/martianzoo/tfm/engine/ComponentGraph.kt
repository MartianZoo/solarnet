package dev.martianzoo.tfm.engine

import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.Multiset
import com.google.common.collect.Multisets
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.types.PetType

class ComponentGraph(startingWith: Collection<Component> = listOf()) {
  private val multiset: /*Mutable*/Multiset<Component> = LinkedHashMultiset.create(startingWith)
  val changeLog: MutableList<StateChange> = mutableListOf() // starts with ordinal 1

  fun gain(count: Int, gaining: Component, cause: Cause? = null) =
      applyChange(count, gaining = gaining, cause = cause)

  fun remove(count: Int, removing: Component, cause: Cause? = null) =
      applyChange(count, removing = removing, cause = cause)

  fun transmute(count: Int, gaining: Component, removing: Component, cause: Cause? = null) =
      applyChange(count, gaining, removing, cause)

  fun applyChange(
      count: Int,
      gaining: Component? = null,
      removing: Component? = null,
      cause: Cause? = null,
  ) {

    // Creating this first should catch various errors
    val change = StateChange(
        ordinal = changeLog.size + 1,
        count = count,
        gaining = gaining?.asTypeExpression as GenericTypeExpression?,
        removing = removing?.asTypeExpression as GenericTypeExpression?,
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

  fun count(type: PetType) =
      multiset.entrySet()
          .filter { it.element.hasType(type) }
          .sumOf { it.count }

  fun getAll(type: PetType): Multiset<Component> =
      Multisets.filter(multiset) { it!!.hasType(type) }

  data class Component(val type: PetType) {
    init {
      require(!type.abstract) { type }
    }

    fun hasType(thatType: PetType) = type.isSubtypeOf(thatType)
    val asTypeExpression = type.toTypeExpressionFull()
    override fun toString() = "[$type]"
  }
}
