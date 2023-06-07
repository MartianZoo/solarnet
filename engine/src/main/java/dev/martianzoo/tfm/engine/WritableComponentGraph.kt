package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.DependencyException
import dev.martianzoo.tfm.api.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.api.TypeInfo.StubTypeInfo
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.engine.ComponentGraph.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Engine.Updater
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Counting
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MClassTable
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
internal class WritableComponentGraph @Inject constructor(internal val effector: Effector) :
    ComponentGraph, Updater {

  private val multiset: HashMultiset<Component> = HashMultiset()

  override operator fun contains(component: Component) = component in multiset.elements

  override fun count(parentType: MType, info: TypeInfo): Int {
    return if (parentType.abstract) {
      getAll(parentType, info).size
    } else {
      countComponent(parentType.toComponent())
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

  // TODO move
  @Singleton
  internal class Limiter
  @Inject
  constructor(private val table: MClassTable, private val components: ComponentGraph) {

    fun findLimit(gaining: Component?, removing: Component?): Int {
      require(gaining != removing) { "$gaining" }

      var actual = Int.MAX_VALUE

      if (gaining != null) {
        val missingDeps = gaining.dependencyComponents.filter { it !in components }
        if (missingDeps.any()) throw DependencyException(missingDeps.map { it.mtype })

        val gainable = gaining.allowedRange.last - components.countComponent(gaining)
        actual = min(actual, gainable)
      }

      if (removing != null) {
        val removable = components.countComponent(removing) - removing.allowedRange.first
        actual = min(actual, removable)
      }

      // MAX 1 Phase, MAX 9 OceanTile
      for (it: Requirement in (table as MClassLoader).generalInvariants) {
        // TODO forbid refinements?
        require(it !is Requirement.Transform)
        if (it is Counting) {
          val supertypeWithLimit = table.resolve(it.scaledEx.expression)
          val gHasType = gaining?.mtype?.narrows(supertypeWithLimit) ?: false
          val rHasType = removing?.mtype?.narrows(supertypeWithLimit) ?: false

          if (gHasType != rHasType) {
            val existing = components.count(supertypeWithLimit, StubTypeInfo)
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
