package dev.martianzoo.engine

import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.HashMultiset
import dev.martianzoo.pets.util.Multiset

/** Component reads from one stable graph amended by this overlay's event-derived count deltas. */
internal class OverlayComponentGraph(
    private val backing: ComponentGraph,
    effector: Effector,
    private val requireUnchangedBacking: () -> Unit,
) : ComponentGraph(effector, backing.classTableForOverlay()) {
  private val classTable = backing.classTableForOverlay()
  private val componentDeltas = mutableMapOf<Component, Int>()

  override operator fun contains(component: Component): Boolean = countComponent(component) > 0

  override fun countComponent(component: Component): Int {
    requireOwnClassTable(component.type)
    requireUnchangedBacking()
    return backing.countComponent(component) + (componentDeltas[component] ?: 0)
  }

  override fun count(parentType: Type, info: TypeInfo): Int {
    requireOwnClassTable(parentType)
    requireUnchangedBacking()
    return if (!classTable.isActive(parentType)) {
      0
    } else if (parentType.className == COMPONENT) {
      backing.count(parentType, info) + componentDeltas.values.sum()
    } else if (parentType.abstract) {
      backing.count(parentType, info) +
          componentDeltas.entries
              .filter { (component, _) -> component.hasType(parentType, info) }
              .sumOf { (_, count) -> count }
    } else {
      countComponent(parentType.toComponent())
    }
  }

  override fun containsAny(parentType: Type, info: TypeInfo): Boolean = count(parentType, info) > 0

  override fun matchingTypes(parentType: Type, info: TypeInfo): Sequence<Type> {
    requireOwnClassTable(parentType)
    requireUnchangedBacking()
    return if (!classTable.isActive(parentType)) {
      emptySequence()
    } else if (parentType.abstract) {
      (backing.matchingTypes(parentType, info) + componentDeltas.keys.asSequence().map { it.type })
          .distinct()
          .filter {
            countComponent(it.toComponent()) > 0 && it.toComponent().hasType(parentType, info)
          }
    } else if (countComponent(parentType.toComponent()) > 0) {
      sequenceOf(parentType)
    } else {
      emptySequence()
    }
  }

  override fun getAll(parentType: Type, info: TypeInfo): Multiset<Component> {
    requireOwnClassTable(parentType)
    requireUnchangedBacking()
    if (!classTable.isActive(parentType)) return HashMultiset()

    return HashMultiset<Component>().also { result ->
      result.addAll(backing.getAll(parentType, info))
      componentDeltas.forEach { (component, delta) ->
        val matches =
            parentType.className == COMPONENT ||
                if (parentType.abstract) component.hasType(parentType, info)
                else component.type == parentType
        if (matches) {
          if (delta >= 0) result.add(component, delta) else result.mustRemove(component, -delta)
        }
      }
    }
  }

  override fun dependentsOf(component: Component): Set<Component> =
      (backing.dependentsOf(component) + super.dependentsOf(component)).filterTo(linkedSetOf()) {
        countComponent(it) > 0
      }

  override fun addToComponentStore(component: Component, count: Int): Int =
      changeCount(component, count)

  override fun removeFromComponentStore(component: Component, count: Int): Int =
      changeCount(component, -count)

  private fun changeCount(component: Component, delta: Int): Int {
    requireUnchangedBacking()
    val newCount = countComponent(component) + delta
    require(newCount >= 0) { "tried to set count of $component to $newCount" }
    val newDelta = (componentDeltas[component] ?: 0) + delta
    if (newDelta == 0) componentDeltas.remove(component) else componentDeltas[component] = newDelta
    return newCount
  }
}
