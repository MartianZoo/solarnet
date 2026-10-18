package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExistingDependentsException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.SystemClasses.SIGNAL
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.HashMultiset
import dev.martianzoo.pets.util.Multiset

/**
 * Live counts of concrete [Component] instances in a game, with links to each component's direct
 * dependencies. A dependency must be present while any dependent remains. Construction seeds a
 * `Class<C>` component for each inhabited concrete class; later game events change the counts.
 * Queries using a [Type][dev.martianzoo.pets.types.Type] include matching subtypes and refinements,
 * while [countComponent] addresses an exact component. Count listeners observe changes to their
 * query after registration; see [listenToCount] for the initial callback and cancellation rules.
 */
public class ComponentGraph
internal constructor(
    private val classTable: ClassTable,
    copyFrom: ComponentGraph? = null,
    private val backing: ComponentGraph? = null,
    private val requireValid: () -> Unit = {},
) {
  /** A removable listener registered with [listenToCount]. */
  public fun interface CountSubscription {
    /** Stops future notifications; it does not retract the initial callback. */
    public fun cancel()
  }

  private val shardClassByClass = mutableMapOf<Class, Class>()
  private val queryShardClassesByClass = mutableMapOf<Class, Set<Class>>()
  private val components: ShardedMultiset<Component, Type, Class> =
      ShardedMultiset<Component, Type, Class>(
              shardFor = { shardClass(it.type.rootClass) },
              queryShardsFor = { queryShardClasses(it.rootClass) },
          )
          .apply {
            if (copyFrom == null && backing == null) {
              classTable.allInhabitedConcreteClasses().forEach { represented ->
                add(classTable.resolve(CLASS.of(represented.className)).toComponent(), 1)
              }
            } else if (copyFrom != null) {
              require(copyFrom.classTable === classTable)
              copyFrom.getAll(classTable.componentClass.baseType, NoGameState).entries.forEach {
                  (component, count) ->
                add(component, count)
              }
            }
          }

  /** Exact count changes relative to the backing graph; absent keys have zero change. */
  private val componentDeltas = mutableMapOf<Component, Int>()
  private var totalDelta = 0

  private val dependentsByDependency: MutableMap<Component, MutableSet<Component>> =
      copyFrom
          ?.takeIf { it.backing == null }
          ?.dependentsByDependency
          ?.mapValuesTo(mutableMapOf()) { (_, dependents) -> dependents.toMutableSet() }
          ?: mutableMapOf()

  init {
    // An overlay's local reverse index omits dependencies inherited from its source. Rebuild
    // those links when materializing a full copy.
    if (copyFrom?.backing != null)
        components.entries.forEach { (component, _) ->
          registerDependencies(component)
        }
  }

  private data class CountListener(
      val type: Type,
      val info: TypeInfo,
      val callback: (Int) -> Unit,
      var lastCount: Int,
  )

  private val countListeners = mutableListOf<CountListener>()

  internal fun fork(): ComponentGraph = ComponentGraph(classTable, copyFrom = this)

  internal fun overlay(requireValid: () -> Unit): ComponentGraph =
      ComponentGraph(classTable, backing = this, requireValid = requireValid)

  internal fun resetOverlayState() {
    check(backing != null)
    requireValid()
    componentDeltas.clear()
    totalDelta = 0
    dependentsByDependency.clear()
  }

  internal fun notifyResetListeners() = notifyCountListeners()

  /**
   * Immediately reports, then observes, the count of [type]. [info] supplies live state for
   * abstract and refined types. Listener failures do not interrupt game mutation.
   */
  public fun listenToCount(
      type: Type,
      info: TypeInfo,
      listener: (Int) -> Unit,
  ): CountSubscription {
    requireOwnClassTable(type)
    val initialCount = count(type, info)
    val registration = CountListener(type, info, listener, initialCount)
    countListeners += registration
    notify(listener, initialCount)
    return CountSubscription {
      countListeners.remove(registration)
    }
  }

  /**
   * Does at least one instance of [component] exist currently? (That is, is [countComponent]
   * nonzero?)
   */
  public operator fun contains(component: Component): Boolean {
    requireOwnClassTable(component.type)
    return countComponent(component) > 0
  }

  /** How many instances of the exact component [component] currently exist? */
  public fun countComponent(component: Component): Int {
    requireOwnClassTable(component.type)
    if (backing != null) requireValid()
    return backing?.countComponent(component)?.plus(componentDeltas[component] ?: 0)
        ?: components.count(component)
  }

  /**
   * How many total component instances have the type [parentType] (or any of its subtypes)? Returns
   * zero for an uninhabited type, which cannot have stored components.
   */
  public fun count(parentType: Type, info: TypeInfo): Int {
    requireOwnClassTable(parentType)
    if (backing != null) requireValid()
    return if (!classTable.isInhabited(parentType)) {
      0
    } else if (parentType.className == COMPONENT && parentType.refinement == null) {
      (backing?.count(parentType, info) ?: components.size) + totalDelta
    } else if (parentType.abstract) {
      if (backing == null) {
        components
            .queryEntries(parentType)
            .filter { (component, _) -> component.hasType(parentType, info) }
            .sumOf { (_, count) -> count }
      } else {
        backing.count(parentType, info) +
            componentDeltas.entries
                .filter { (component, _) -> component.hasType(parentType, info) }
                .sumOf { (_, delta) -> delta }
      }
    } else {
      countComponent(parentType.toComponent())
    }
  }

  /**
   * Whether this graph currently has any component matching [parentType], including subtypes and
   * refinements evaluated with [info]. Returns false for an uninhabited type.
   */
  public fun containsAny(parentType: Type, info: TypeInfo): Boolean {
    requireOwnClassTable(parentType)
    if (backing != null) return count(parentType, info) > 0
    return if (!classTable.isInhabited(parentType)) {
      false
    } else if (parentType.abstract) {
      components.queryElements(parentType).any { it.hasType(parentType, info) }
    } else {
      parentType.toComponent() in components
    }
  }

  /** Whether [dependencyType] has a direct or indirect live dependent matching [dependentType]. */
  public fun hasDependentMatching(
      dependencyType: Type,
      dependentType: Type,
      info: TypeInfo,
  ): Boolean {
    requireOwnClassTable(dependencyType)
    requireOwnClassTable(dependentType)
    val visited = mutableSetOf<Component>()
    fun hasMatchingDependent(dependency: Component): Boolean =
        dependentsOf(dependency).any {
          visited.add(it) && (it.hasType(dependentType, info) || hasMatchingDependent(it))
        }
    return hasMatchingDependent(dependencyType.toComponent())
  }

  /** Distinct concrete component Types currently matching [parentType]. */
  public fun matchingTypes(parentType: Type, info: TypeInfo): Sequence<Type> {
    requireOwnClassTable(parentType)
    if (backing != null) requireValid()
    if (backing != null) {
      if (!classTable.isInhabited(parentType)) return emptySequence()
      if (!parentType.abstract) {
        return if (countComponent(parentType.toComponent()) > 0) sequenceOf(parentType)
        else emptySequence()
      }
      return (backing.matchingTypes(parentType, info) +
              componentDeltas.keys.asSequence().map { it.type })
          .distinct()
          .filter {
            countComponent(it.toComponent()) > 0 && it.toComponent().hasType(parentType, info)
          }
    }
    return if (!classTable.isInhabited(parentType)) {
      emptySequence()
    } else if (parentType.abstract) {
      components.queryElements(parentType).filter { it.hasType(parentType, info) }.map { it.type }
    } else if (parentType.toComponent() in components) {
      sequenceOf(parentType)
    } else {
      emptySequence()
    }
  }

  /**
   * Returns all component instances having the type [parentType] (or any of its subtypes), as a
   * multiset. The size of the returned collection will be `[count]([parentType])` . An uninhabited
   * type returns an empty multiset. If [parentType] is `Component` this returns the entire
   * component multiset. A refined `Component` is filtered like every other abstract Type.
   */
  public fun getAll(parentType: Type, info: TypeInfo): Multiset<Component> {
    requireOwnClassTable(parentType)
    if (backing != null) requireValid()
    if (!classTable.isInhabited(parentType)) return HashMultiset()
    if (backing != null) {
      val result = HashMultiset<Component>()
      result.addAll(backing.getAll(parentType, info))
      componentDeltas.forEach { (component, delta) ->
        val matches =
            parentType.className == COMPONENT && parentType.refinement == null ||
                if (parentType.abstract) component.hasType(parentType, info)
                else component.type == parentType
        if (matches) {
          if (delta > 0) result.add(component, delta) else result.mustRemove(component, -delta)
        }
      }
      return result
    }
    return if (parentType.className == COMPONENT && parentType.refinement == null) {
      components.copy()
    } else if (parentType.abstract) {
      components.filter(parentType) { it.hasType(parentType, info) }
    } else {
      val component = parentType.toComponent()
      HashMultiset<Component>().also { it.add(component, components.count(component)) }
    }
  }

  /** Removes and/or gains [count] copies while keeping structural indexes synchronized. */
  internal fun applyChange(count: Int, gaining: Component?, removing: Component?) {
    if (
        gaining != null &&
            gaining == removing &&
            !gaining.type.rootClass.isSubtypeOf(classTable.getClass(SIGNAL))
    ) {
      throw ExpressionException("a transmutation must change its type: $gaining")
    }
    listOfNotNull(gaining, removing).forEach {
      requireOwnClassTable(it.type)
      if (!classTable.isInhabited(it.type)) {
        throw ExpressionException("uninhabited type has no components: ${it.type}")
      }
      if (it.type.rootClass.declaration.customMetric) {
        throw ExpressionException(
            "Custom metric `${it.expressionFull}` cannot enter ComponentGraph"
        )
      }
    }
    // This is the authoritative state invariant. Live engines may predict it while resolving an
    // instruction, but passive replay and engine-free GameWorld use cannot rely on an engine check.
    gaining?.let { component ->
      val missing =
          component.dependencyComponents.filter { dependency ->
            val removed = if (dependency == removing) count else 0
            countComponent(dependency) - removed <= 0
          }
      if (missing.isNotEmpty()) throw DependencyException(missing.map { it.type })
    }
    if (gaining == removing) return
    removing?.let {
      checkDependents(count, it)
      val remaining = changeCount(it, -count)
      if (remaining == 0) unregisterDependencies(it)
    }
    gaining?.let {
      val newCount = changeCount(it, count)
      if (newCount == count && count > 0) registerDependencies(it)
    }
    notifyCountListeners()
  }

  private fun notifyCountListeners() {
    countListeners.toList().forEach { registration ->
      val count = count(registration.type, registration.info)
      if (count != registration.lastCount) {
        registration.lastCount = count
        notify(registration.callback, count)
      }
    }
  }

  private fun notify(listener: (Int) -> Unit, count: Int) {
    try {
      listener(count)
    } catch (_: Throwable) {
      // Observation must not make an already-applied state change fail.
    }
  }

  private fun changeCount(component: Component, delta: Int): Int {
    if (backing == null) {
      return if (delta > 0) components.add(component, delta)
      else components.mustRemove(component, -delta)
    }
    requireValid()
    val newCount = countComponent(component) + delta
    require(newCount >= 0) { "tried to set count of $component to $newCount" }
    val newDelta = (componentDeltas[component] ?: 0) + delta
    if (newDelta == 0) componentDeltas.remove(component) else componentDeltas[component] = newDelta
    totalDelta += delta
    return newCount
  }

  private fun requireOwnClassTable(type: Type) {
    require(classTable.knows(type)) { "$type belongs to a different Catalog" }
  }

  private fun queryShardClasses(klass: Class): Set<Class> =
      queryShardClassesByClass.getOrPut(klass) {
        // An abstract query can cross a later inheritance junction, so include the shard of every
        // possible root subclass. The shards partition components, so summing them is safe.
        classTable.allSubclasses(klass).mapTo(linkedSetOf(), ::shardClass)
      }

  private fun shardClass(klass: Class): Class =
      shardClassByClass.getOrPut(klass) {
        // Collapse a single-inheritance chain into one shard, stopping at Component or at the
        // first inheritance junction. Every class therefore has exactly one shard.
        val parents = klass.directSuperclasses
        if (classTable.componentClass in parents || parents.size != 1) {
          klass
        } else {
          shardClass(parents.single())
        }
      }

  private fun checkDependents(count: Int, removing: Component) {
    if (countComponent(removing) == count) {
      val dependents = dependentsOf(removing)
      if (dependents.isNotEmpty()) {
        throw ExistingDependentsException(dependents.map { it.type })
      }
    }
  }

  /**
   * Exact component types that currently depend directly on [component]. The returned set may
   * reflect later graph changes; copy it if a stable snapshot is needed.
   */
  public fun dependentsOf(component: Component): Set<Component> =
      if (backing == null) {
        dependentsByDependency[component].orEmpty()
      } else {
        requireValid()
        (backing.dependentsOf(component) + dependentsByDependency[component].orEmpty()).filterTo(
            linkedSetOf()
        ) {
          countComponent(it) > 0
        }
      }

  private fun registerDependencies(dependent: Component) {
    dependent.type.typeDependencies.forEach { dependency ->
      dependentsByDependency
          .getOrPut(dependency.boundType.toComponent(), ::linkedSetOf)
          .add(dependent)
    }
  }

  private fun unregisterDependencies(dependent: Component) {
    dependent.type.typeDependencies.forEach { dependency ->
      val dependencyComponent = dependency.boundType.toComponent()
      dependentsByDependency[dependencyComponent]?.let { dependents ->
        dependents.remove(dependent)
        if (dependents.isEmpty()) dependentsByDependency.remove(dependencyComponent)
      }
    }
  }
}
