package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.types.ClassLimitTable
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.toComponent
import kotlin.Int.Companion.MAX_VALUE

internal class Limiter(
    private val classTable: ClassTable,
    private val gameWorld: GameWorld,
) {
  private val limits: ClassLimitTable = classTable.componentLimits

  // Bootstrap constructs an incomplete world and audits all required counts when it finishes.
  internal var checkRequiredCounts: Boolean = false

  /** Audits the complete World, including required components that are absent. */
  internal fun checkAllInvariants() {
    val liveTypes =
        gameWorld.components.getAll(classTable.componentClass.baseType, NoGameState).elements.map {
          it.type
        }
    val allLimits = liveTypes.flatMap(limits::limitsFor).toSet() + limits.requiredLimits(liveTypes)
    for (limit in allLimits) {
      val count = gameWorld.components.count(limit.type, NoGameState)
      if (count !in limit.range) {
        throw LimitsException(
            "component count invariant violated: `${limit.type.expression}` " +
                "(found $count, expected ${limit.range})"
        )
      }
    }
  }

  internal fun checkInvariantsSince(checkpoint: Checkpoint) {
    val changed =
        gameWorld.events
            .changesSince(checkpoint)
            .flatMap { event ->
              listOfNotNull(event.change.gaining, event.change.removing)
            }
            .toSet()
    val required =
        if (checkRequiredCounts) {
          // A requirement can belong to any live ancestor of a changed or removed component.
          // Include new owners even when none of their required dependents was created.
          val scopes = mutableSetOf<Component>()
          fun addScope(component: Component) {
            if (scopes.add(component)) component.dependencyComponents.forEach(::addScope)
          }
          changed.forEach(::addScope)
          val liveScopes = scopes.filter { it in gameWorld.components }.map { it.type }
          limits.requiredLimits(liveScopes, changedTypes = changed.map { it.type })
        } else {
          emptySet()
        }
    val touched = changed.flatMap { limitsFor(it) }.toSet()
    for (limit in touched + required) {
      if (limit.range.first == 0 && limit.range.last == MAX_VALUE) continue
      val count = gameWorld.components.count(limit.type, NoGameState)
      if (count > limit.range.last || (limit in required && count < limit.range.first)) {
        throw LimitsException(
            "component count invariant violated: `${limit.type.expression}` " +
                "(found $count, expected ${limit.range})"
        )
      }
    }
  }

  /** Physical capacity, also bounded by current count invariants for AMAP/optional quantities. */
  internal fun findLimit(
      gaining: Component?,
      removing: Component?,
      invariants: Boolean,
  ): Int {
    val missingDeps = missingDependencies(gaining)
    if (missingDeps.any()) throw DependencyException(missingDeps.map { it.type })

    return findLimitWithDependenciesPresent(gaining, removing, invariants)
  }

  private fun findLimitWithDependenciesPresent(
      gaining: Component?,
      removing: Component?,
      invariants: Boolean,
  ): Int {

    // A same-Type exchange leaves every invariant unchanged, but requires the source count.
    if (gaining != null && gaining == removing) return gameWorld.components.countComponent(gaining)

    // We must ignore any that are in common; the transmutation must hold them constant
    val (gainInvars, removeInvars) =
        run {
          val g = if (invariants) limitsFor(gaining) else emptySet()
          val r = if (invariants) limitsFor(removing) else emptySet()
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
    val available = removing?.let(gameWorld.components::countComponent) ?: MAX_VALUE
    return (headroom + footroom + dependencyFootroom + available).min().coerceAtLeast(0)
  }

  private fun missingDependencies(gaining: Component?): List<Component> =
      gaining?.dependencyComponents?.filterNot { it in gameWorld.components }.orEmpty()

  internal fun hasComponents(type: Type, info: TypeInfo): Boolean =
      gameWorld.components.containsAny(type, info)

  /**
   * Narrows a gain when present dependencies identify one target after task selection. Declared
   * count capacity does not select a target; an at-most-one dependency can identify a live target.
   */
  internal fun singleConcreteGainWithPresentDependencies(
      type: Type,
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
        .take(2)
        .singleOrNull()
  }

  internal fun hasAvailableConcreteGain(
      type: Type,
      minimum: Int,
      info: TypeInfo,
      invariants: Boolean,
  ): Boolean {
    require(minimum > 0)
    return classTable
        .allConcreteSubtypes(type) { dependency ->
          gameWorld.components.matchingTypes(dependency, info)
        }
        .any { candidate ->
          candidate.narrows(type, info) &&
              findLimitWithDependenciesPresent(candidate.toComponent(), null, invariants) >= minimum
        }
  }

  internal fun hasAvailableConcreteRemoval(
      type: Type,
      minimum: Int,
      info: TypeInfo,
      invariants: Boolean,
  ): Boolean {
    require(minimum > 0)
    return gameWorld.components.matchingTypes(type, info).any { candidate ->
      findLimit(null, candidate.toComponent(), invariants) >= minimum
    }
  }

  private fun limitsFor(component: Component?): Set<ClassLimitTable.Limit> {
    val type = component?.type ?: return emptySet()
    return limits.limitsFor(type)
  }
}
