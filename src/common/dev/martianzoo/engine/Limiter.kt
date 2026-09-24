package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.types.ClassLimitTable
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.toComponent
import kotlin.Int.Companion.MAX_VALUE

internal class Limiter(
    private val classTable: ClassTable,
    private val gameWorld: GameWorld,
) {
  private val limits: ClassLimitTable = classTable.componentLimits

  internal fun findLimit(gaining: Component?, removing: Component?): Int {
    val missingDeps = missingDependencies(gaining)
    if (missingDeps.any()) throw DependencyException(missingDeps.map { it.type })

    return findLimitWithDependenciesPresent(gaining, removing)
  }

  internal fun findLimitOrNull(gaining: Component?, removing: Component?): Int? {
    if (missingDependencies(gaining).any()) return null

    return findLimitWithDependenciesPresent(gaining, removing)
  }

  private fun findLimitWithDependenciesPresent(
      gaining: Component?,
      removing: Component?,
  ): Int {

    // We must ignore any that are in common; the transmutation must hold them constant
    val (gainInvars, removeInvars) =
        run {
          val g = limitsFor(gaining)
          val r = limitsFor(removing)
          (g - r) to (r - g)
        }

    fun count(type: Type) = gameWorld.components.count(type, NoGameState)

    val headroom = gainInvars.map { it.range.last - count(it.type) }
    val footroom = removeInvars.map { count(it.type) - it.range.first }
    // Predict GameWorld's dependency invariant so a concrete selected transmutation cannot fail
    // only when it reaches passive state application.
    val dependencyFootroom =
        gaining
            ?.dependencyComponents
            ?.filter { it == removing }
            ?.map { gameWorld.components.countComponent(it) - 1 }
            .orEmpty()
    return (headroom + footroom + dependencyFootroom).minOrNull() ?: MAX_VALUE
  }

  private fun missingDependencies(gaining: Component?): List<Component> =
      gaining?.dependencyComponents?.filterNot { it in gameWorld.components }.orEmpty()

  /**
   * Narrows a gain using present dependency targets and applicable limits after task selection. An
   * at-most-one nested dependency can also identify an existing target.
   */
  internal fun singleConcreteGainWithPresentDependencies(
      type: Type,
      removing: Type?,
      info: TypeInfo,
  ): Type? {
    val addedGainDependency =
        type.rootClass.abstract &&
            classTable
                .allSubclasses(type.rootClass)
                .filterNot { it.abstract }
                .any { subclass ->
                  subclass.dependencies.typeDependencies().any { dependency ->
                    dependency.key !in type.dependencies.keys &&
                        (gameWorld.components.matchingTypes(dependency.boundType, info).none() ||
                            !dependency.boundType.rootClass.abstract)
                  }
                }
    val uniqueExistingDependency =
        type.dependencies.typeDependencies().any { dependency ->
          val target = dependency.boundType
          target.abstract &&
              limits.limitsFor(target).any { it.range.last <= 1 } &&
              gameWorld.components.matchingTypes(target, info).take(2).count() == 1
        }
    if (!addedGainDependency && !uniqueExistingDependency) return null
    return classTable
        .allConcreteSubtypes(type) { dependency ->
          gameWorld.components.matchingTypes(dependency, info)
        }
        .filter { it.narrows(type, info) }
        .filter {
          removing?.abstract == true ||
              findLimitWithDependenciesPresent(it.toComponent(), removing?.toComponent()) > 0
        }
        .take(2)
        .singleOrNull()
  }

  internal fun hasExecutableConcreteGain(
      type: Type,
      minimum: Int,
      info: TypeInfo,
  ): Boolean {
    require(type.abstract)
    require(minimum > 0)
    return classTable
        .allConcreteSubtypes(type) { dependency ->
          gameWorld.components.matchingTypes(dependency, info)
        }
        .any { candidate ->
          candidate.narrows(type, info) &&
              findLimitWithDependenciesPresent(candidate.toComponent(), null) >= minimum
        }
  }

  internal fun hasExecutableConcreteRemoval(
      type: Type,
      minimum: Int,
      info: TypeInfo,
  ): Boolean {
    require(type.abstract)
    require(minimum > 0)
    return gameWorld.components.matchingTypes(type, info).any { candidate ->
      findLimit(null, candidate.toComponent()) >= minimum
    }
  }

  private fun limitsFor(component: Component?): Set<ClassLimitTable.Limit> {
    val type = component?.type ?: return emptySet()
    return limits.limitsFor(type)
  }
}
