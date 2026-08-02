package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExistingDependentsException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.engine.Engine.Updater
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Type
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset

/** Internal mutation capability paired with the public read-only [ComponentGraph]. */
internal interface WritableComponentGraph : ComponentGraph, Updater {

  /** Component graph backed by the complete live game's component multiset. */
  class Whole(private val effector: Effector, private val classTable: ClassTable) :
      WritableComponentGraph {

    private val multiset: HashMultiset<Component> = HashMultiset()

    override operator fun contains(component: Component): Boolean {
      requireOwnClassTable(component.type)
      return component in multiset.elements
    }

    override fun count(parentType: Type, info: TypeInfo): Int {
      requireOwnClassTable(parentType)
      return if (parentType.phantom) {
        0
      } else if (parentType.className == COMPONENT) {
        multiset.size
      } else if (parentType.abstract) {
        multiset.entries.filter { (e, _) -> e.hasType(parentType, info) }.sumOf { (_, ct) -> ct }
      } else {
        countComponent(parentType.toComponent())
      }
    }

    override fun containsAny(parentType: Type, info: TypeInfo): Boolean {
      requireOwnClassTable(parentType)
      return if (parentType.phantom) {
        false
      } else if (parentType.abstract) {
        multiset.elements.any { it.hasType(parentType, info) }
      } else {
        parentType.toComponent() in multiset
      }
    }

    override fun countComponent(component: Component): Int {
      requireOwnClassTable(component.type)
      return multiset.count(component)
    }

    override fun getAll(parentType: Type, info: TypeInfo): Multiset<Component> {
      requireOwnClassTable(parentType)
      return if (parentType.phantom) {
        HashMultiset()
      } else if (parentType.className == COMPONENT) {
        HashMultiset.of(multiset)
      } else if (parentType.abstract) {
        multiset.filter { it.hasType(parentType, info) }
      } else {
        val cpt = parentType.toComponent()
        HashMultiset<Component>().also { it.add(cpt, multiset.count(cpt)) }
      }
    }

    override fun update(count: Int, gaining: Component?, removing: Component?): StateChange {
      listOfNotNull(gaining, removing).forEach {
        requireOwnClassTable(it.type)
        if (it.isCustom) {
          throw ExpressionException(
              "Custom component `${it.expressionFull}` cannot enter ComponentGraph"
          )
        }
      }
      removing?.let { r ->
        checkDependents(count, r)
        multiset.mustRemove(r, count)
        effector.mustRemove(r, count)
      }
      gaining?.let { g ->
        multiset.add(g, count)
        effector.add(g, count)
      }
      return StateChange(count, gaining?.expressionFull, removing?.expressionFull)
    }

    private fun requireOwnClassTable(type: Type) {
      require(type.classTable === classTable) { "$type belongs to a different class table" }
    }

    private fun checkDependents(count: Int, removing: Component) {
      if (countComponent(removing) == count) {
        if (multiset.elements.any { removing in it.dependencyComponents }) {
          val dependents = multiset.elements.filter { removing in it.dependencyComponents }
          throw ExistingDependentsException(dependents.map { it.type })
        }
      }
    }
  }
}
