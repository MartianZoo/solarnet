package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.tfm.engine.Engine.GameScoped
import dev.martianzoo.tfm.engine.Engine.Updater
import dev.martianzoo.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import javax.inject.Inject

@GameScoped
internal class WritableComponentGraph @Inject constructor(internal val effector: Effector) :
    ComponentGraph, Updater {

  private val multiset: HashMultiset<Component> = HashMultiset()

  override operator fun contains(component: Component) = component in multiset.elements

  override fun count(parentType: MType, info: TypeInfo): Int {
    return if (parentType.className == COMPONENT) {
      multiset.size
    } else if (parentType.abstract) {
      multiset.entries
          .filter { (e, _) -> e.mtype.narrows(parentType, info) }
          .sumOf { (_, ct) -> ct }
    } else {
      countComponent(parentType.toComponent())
    }
  }

  override fun containsAny(parentType: MType, info: TypeInfo): Boolean {
    return if (parentType.abstract) {
      multiset.elements.any { it.mtype.narrows(parentType, info) }
    } else {
      parentType.toComponent() in multiset
    }
  }

  override fun countComponent(component: Component) = multiset.count(component)

  override fun getAll(parentType: MType, info: TypeInfo): Multiset<Component> {
    return if (parentType.className == COMPONENT) {
      HashMultiset.of(multiset)
    } else if (parentType.abstract) {
      multiset.filter { it.mtype.narrows(parentType, info) }
    } else {
      val cpt = parentType.toComponent()
      HashMultiset<Component>().also { it.add(cpt, multiset.count(cpt)) }
    }
  }

  override fun update(count: Int, gaining: Component?, removing: Component?): StateChange {
    removing?.let { r ->
      checkDependents(count, r)
      multiset.mustRemove(r, count)
      effector.update(r, -count)
    }
    gaining?.let { g ->
      multiset.add(g, count)
      effector.update(g, count)
    }
    return StateChange(count, gaining?.expressionFull, removing?.expressionFull)
  }

  private fun checkDependents(count: Int, removing: Component) {
    if (countComponent(removing) == count) {
      if (multiset.elements.any { removing in it.dependencyComponents }) {
        val dependents = multiset.elements.filter { removing in it.dependencyComponents }
        throw ExistingDependentsException(dependents.map { it.mtype })
      }
    }
  }
}
