package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.DependencyException
import dev.martianzoo.tfm.api.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.api.TypeInfo.StubTypeInfo
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Counting
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import kotlin.Int.Companion.MAX_VALUE
import kotlin.math.min

internal class WritableComponentGraph(private val effector: Effector) : ComponentGraph {

  private val multiset: HashMultiset<Component> = HashMultiset()

  override operator fun contains(component: Component) = component in multiset.elements

  override fun count(parentType: MType, info: TypeInfo) = getAll(parentType, info).size
  override fun countComponent(component: Component) = multiset.count(component)

  override fun getAll(parentType: MType, info: TypeInfo): Multiset<Component> {
    return multiset.filter { it.mtype.narrows(parentType, info) }
  }

  internal fun update(count: Int = 1, gaining: Component?, removing: Component?): StateChange {
    removing?.let { r ->
      multiset.mustRemove(r, count)
      r.activeEffects.forEach { effector.update(it, -count) }
    }
    gaining?.let { g ->
      multiset.add(g, count)
      g.activeEffects.forEach { effector.update(it, count) }
    }
    return StateChange(count, gaining?.expressionFull, removing?.expressionFull)
  }

  override fun findLimit(gaining: Component?, removing: Component?): Int {
    require(gaining != removing)

    var actual = MAX_VALUE
    val loader = (gaining ?: removing)!!.mtype.loader

    if (gaining != null) {
      val missingDeps = gaining.dependencyComponents - multiset.elements
      if (missingDeps.any()) throw DependencyException(missingDeps.map { it.mtype })

      val gainable = gaining.allowedRange.last - countComponent(gaining)
      actual = min(actual, gainable)
    }

    if (removing != null) {
      val removable = countComponent(removing) - removing.allowedRange.first
      actual = min(actual, removable)
    }

    // MAX 1 Phase, MAX 9 OceanTile
    for (it: Requirement in loader.generalInvariants) {
      // TODO forbid refinements?
      require(it !is Requirement.Transform)
      if (it is Counting) {
        val supertypeWithLimit = loader.resolve(it.scaledEx.expression)
        val gHasType = gaining?.mtype?.narrows(supertypeWithLimit) ?: false
        val rHasType = removing?.mtype?.narrows(supertypeWithLimit) ?: false

        if (gHasType != rHasType) {
          val existing = count(supertypeWithLimit, StubTypeInfo)
          if (gHasType) {
            val gainable = it.range.last - existing
            actual = min(actual, gainable)
          }
          if (rHasType) {
            val removable = existing - it.range.first
            actual = min(actual, removable)
          }
        }
      }
    }
    return actual
  }

  fun checkDependents(count: Int, removing: Component) {
    if (countComponent(removing) == count) {
      val dependents = multiset.filter { removing in it.dependencyComponents }
      if (dependents.any()) {
        throw ExistingDependentsException(dependents.elements.map { it.mtype })
      }
    }
  }
}
