package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.ExpressionInfo.StubExpressionInfo
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.engine.Exceptions.DependencyException
import dev.martianzoo.tfm.engine.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.engine.Exceptions.LimitsException
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Counting
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import kotlin.math.min

internal class WritableComponentGraph(
    private val multiset: HashMultiset<Component> = HashMultiset(),
) : ComponentGraph {
  override operator fun contains(component: Component) = component in multiset.elements

  override fun count(parentType: MType, einfo: ExpressionInfo) = getAll(parentType, einfo).size
  override fun countComponent(component: Component) = multiset.count(component)

  override fun getAll(parentType: MType, einfo: ExpressionInfo): Multiset<Component> {
    // TODO: refinement-aware
    return multiset.filter {
      try {
        it.mtype.ensureNarrows(parentType, einfo)
        true
      } catch (e: Exception) {
        false
      }
    }
  }

  // TODO update this redundantly instead of walking the whole table?
  fun activeEffects(classes: Set<MClass>): List<ActiveEffect> {
    val superclasses = classes.flatMap { it.allSuperclasses }.toSet().classNames()
    return multiset
        .flatMap { cpt -> cpt.activeEffects.filter { it.classToCheck in superclasses } }
        .entries
        .map { (effect, count) -> effect * count }
  }

  internal fun update(
      count: Int = 1,
      gaining: Component? = null,
      removing: Component? = null,
      amap: Boolean,
  ): StateChange? {
    require(gaining != removing)
    val actual = checkLimitsAndDeps(count, gaining, removing, amap)
    if (actual == 0) return null

    removing?.let { multiset.mustRemove(it, actual) }
    gaining?.let { multiset.add(it, actual) }

    return StateChange(
        count = actual,
        gaining = gaining?.expression,
        removing = removing?.expression,
    )
  }

  private fun checkLimitsAndDeps(
      count: Int,
      gaining: Component?,
      removing: Component?,
      toTheExtentPossible: Boolean,
  ): Int {
    require(count >= 1)
    var actual = count
    val loader = (gaining ?: removing)!!.mtype.loader

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
        val dependents = multiset.filter { removing in it.dependencyComponents }
        if (dependents.any()) {
          throw ExistingDependentsException(dependents)
        }
      }
    }

    // MAX 1 Phase, MAX 9 OceanTile
    for (it: Requirement in loader.generalInvariants) {
      // TODO forbid refinements?
      require(it !is Requirement.Transform)
      if (it is Counting) {
        val supertypeWithLimit = loader.resolve(it.scaledEx.expression)
        val gHasType = gaining?.mtype?.isSubtypeOf(supertypeWithLimit) ?: false
        val rHasType = removing?.mtype?.isSubtypeOf(supertypeWithLimit) ?: false

        if (gHasType != rHasType) {
          val existing = count(supertypeWithLimit, StubExpressionInfo)
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

    if (!toTheExtentPossible && actual != count) {
      throw LimitsException(
          "When gaining $gaining and removing $removing: can do only $actual of $count required")
    }
    return actual
  }

  internal fun reverse(
      count: Int,
      removeWhatWasGained: Component? = null,
      gainWhatWasRemoved: Component? = null,
  ) {
    removeWhatWasGained?.let { multiset.mustRemove(it, count) }
    gainWhatWasRemoved?.let { multiset.add(it, count) }
  }

  fun clone() = WritableComponentGraph(multiset.clone())
}
