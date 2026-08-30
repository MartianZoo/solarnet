package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement.Counting
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassLimitTable
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import kotlin.Int.Companion.MAX_VALUE

internal class Limiter(
    private val classTable: ClassTable,
    private val components: ComponentGraph,
) {
  private val limits: ClassLimitTable = classTable.componentLimits

  internal fun findLimit(gaining: Component?, removing: Component?): Int {
    if (gaining != null) {
      val missingDeps = gaining.dependencyComponents.filter { it !in components }
      if (missingDeps.any()) throw DependencyException(missingDeps.map { it.type })
    }

    // We must ignore any that are in common; the transmutation must hold them constant
    val (gainInvars, removeInvars) =
        run {
          val g = limitsFor(gaining)
          val r = limitsFor(removing)
          (g - r) to (r - g)
        }

    fun count(type: Type) = components.count(type, NoGameState)

    val headroom = gainInvars.map { it.range.last - count(it.type) }
    val footroom = removeInvars.map { count(it.type) - it.range.first }
    return (headroom + footroom).minOrNull() ?: MAX_VALUE
  }

  internal fun hasExecutableConcreteGain(
      type: Type,
      minimum: Int,
      info: TypeInfo,
  ): Boolean {
    require(type.abstract)
    require(minimum > 0)
    return classTable.allConcreteSubtypes(type).any { candidate ->
      candidate.narrows(type, info) &&
          try {
            findLimit(candidate.toComponent(), null) >= minimum
          } catch (_: DependencyException) {
            false
          }
    }
  }

  internal fun hasExecutableConcreteRemoval(
      type: Type,
      minimum: Int,
      info: GameReader,
  ): Boolean {
    require(type.abstract)
    require(minimum > 0)
    return info.getComponents(type).elements.any { candidate ->
      findLimit(null, candidate.toComponent()) >= minimum
    }
  }

  private fun limitsFor(component: Component?): Set<ClassLimitTable.Limit> {
    val type = component?.type ?: return emptySet()
    return limits.limitsFor(type)
  }
}

internal fun Class.isSingletonType(): Boolean = invariants.any {
  val counting = it as? Counting ?: return@any false
  counting.range.first == 1 && (counting.metric as? Metric.Count)?.expression == THIS.expression
}
