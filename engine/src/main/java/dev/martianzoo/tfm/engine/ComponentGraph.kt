package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.engine.Exceptions.DependencyException
import dev.martianzoo.tfm.engine.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.engine.Exceptions.LimitsException
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.MutableMultiset
import kotlin.math.min

/** All the components making up the state of a single [Game]. */
public class ComponentGraph {
  private val multiset: MutableMultiset<Component> = HashMultiset()

  public operator fun contains(component: Component) = component in multiset.elements

  public fun count(mtype: MType) = getAll(mtype).size
  public fun countComponent(component: Component) = multiset.count(component)

  public fun getAll(mtype: MType): Multiset<Component> = multiset.filter { it.hasType(mtype) }

  internal fun allActiveEffects(game: Game): Multiset<ActiveEffect> =
      multiset.flatMap { it.effects(game) }

  internal fun update(
      count: Int = 1,
      gaining: Component? = null,
      removing: Component? = null,
      toTheExtentPossible: Boolean,
  ): StateChange? {
    require(count >= 1)
    var actual = count

    if (gaining != null) {
      val missingDeps = gaining.dependencyComponents - multiset.elements
      if (missingDeps.any()) throw DependencyException(missingDeps)

      val gainable = gaining.allowedRange.last - countComponent(gaining)
      actual = min(actual, gainable)
    }

    if (removing != null) {
      val removable = countComponent(removing) - removing.allowedRange.first
      actual = min(actual, removable)

      if (actual == removable) { // if we're removing them all
        val dependents = dependentsOf(removing)
        if (dependents.any()) {
          throw ExistingDependentsException(dependents)
        }
      }
    }

    if (!toTheExtentPossible && actual != count) {
      throw LimitsException("can't gain/remove $count instances, only $actual")
    }

    if (actual == 0) return null

    updateMultiset(actual, gaining, removing)
    return StateChange(
        count = actual,
        gaining = gaining?.mtype?.expression,
        removing = removing?.mtype?.expression,
    )
  }

  // Only called above and by rollback()
  internal fun updateMultiset(
      count: Int,
      gaining: Component? = null,
      removing: Component? = null,
  ) {
    require(gaining != removing)
    removing?.let { multiset.mustRemove(it, count) }
    gaining?.let { multiset.add(it, count) }
  }

  internal fun dependentsOf(dependency: Component) =
      multiset.filter { dependency in it.dependencyComponents }
}
