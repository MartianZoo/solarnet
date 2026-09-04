package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.ExistingDependentsException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.HashMultiset
import dev.martianzoo.pets.util.Multiset

/**
 * A multiset of [Component] instances; the "present" state of a game in progress. It is a plain
 * multiset, but called a "graph" because these component instances have references to their
 * dependencies which are also stored in the multiset.
 */
public class ComponentGraph
private constructor(
    private val classTable: ClassTable,
    private val addEffects: (Component, Int) -> Unit,
    private val removeEffects: (Component, Int) -> Unit,
) {
  /** A removable listener registered with [listenToCount]. */
  public fun interface CountSubscription {
    public fun cancel()
  }

  internal constructor(
      effector: Effector,
      classTable: ClassTable,
  ) : this(classTable, effector::add, effector::mustRemove)

  private val shardClassByClass = mutableMapOf<Class, Class>()
  private val queryShardClassesByClass = mutableMapOf<Class, Set<Class>>()
  private val components =
      ShardedMultiset<Component, Type, Class>(
          shardFor = { shardClass(it.type.rootClass) },
          queryShardsFor = { queryShardClasses(it.rootClass) },
      )
  private val dependentsByDependency = mutableMapOf<Component, MutableSet<Component>>()

  private data class CountListener(
      val type: Type,
      val info: TypeInfo,
      val callback: (Int) -> Unit,
      var lastCount: Int,
  )

  private val countListeners = mutableListOf<CountListener>()

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
  internal operator fun contains(component: Component): Boolean {
    requireOwnClassTable(component.type)
    return component in components
  }

  /** How many instances of the exact component [component] currently exist? */
  internal fun countComponent(component: Component): Int {
    requireOwnClassTable(component.type)
    return components.count(component)
  }

  /**
   * How many total component instances have the type [parentType] (or any of its subtypes)? Returns
   * zero for an inactive type, which cannot have stored components.
   */
  internal fun count(parentType: Type, info: TypeInfo): Int {
    requireOwnClassTable(parentType)
    return if (!classTable.isActive(parentType)) {
      0
    } else if (parentType.className == COMPONENT) {
      components.size
    } else if (parentType.abstract) {
      components
          .queryEntries(parentType)
          .filter { (component, _) -> component.hasType(parentType, info) }
          .sumOf { (_, count) -> count }
    } else {
      countComponent(parentType.toComponent())
    }
  }

  internal fun containsAny(parentType: Type, info: TypeInfo): Boolean {
    requireOwnClassTable(parentType)
    return if (!classTable.isActive(parentType)) {
      false
    } else if (parentType.abstract) {
      components.queryElements(parentType).any { it.hasType(parentType, info) }
    } else {
      parentType.toComponent() in components
    }
  }

  /** Distinct concrete component Types currently matching [parentType]. */
  internal fun matchingTypes(parentType: Type, info: TypeInfo): Sequence<Type> {
    requireOwnClassTable(parentType)
    return if (!classTable.isActive(parentType)) {
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
   * multiset. The size of the returned collection will be `[count]([parentType])` . An inactive
   * type returns an empty multiset. If [parentType] is `Component` this returns the entire
   * component multiset.
   */
  internal fun getAll(parentType: Type, info: TypeInfo): Multiset<Component> {
    requireOwnClassTable(parentType)
    return if (!classTable.isActive(parentType)) {
      HashMultiset()
    } else if (parentType.className == COMPONENT) {
      components.copy()
    } else if (parentType.abstract) {
      components.filter(parentType) { it.hasType(parentType, info) }
    } else {
      val component = parentType.toComponent()
      HashMultiset<Component>().also { it.add(component, components.count(component)) }
    }
  }

  /** Removes and/or gains [count] copies while keeping live-effect indexes synchronized. */
  internal fun applyChange(count: Int, gaining: Component?, removing: Component?) {
    listOfNotNull(gaining, removing).forEach {
      requireOwnClassTable(it.type)
      if (!classTable.isActive(it.type)) {
        throw ExpressionException("inactive type has no components: ${it.type}")
      }
      if (it.isCustom) {
        throw ExpressionException(
            "Custom component `${it.expressionFull}` cannot enter ComponentGraph"
        )
      }
    }
    removing?.let {
      checkDependents(count, it)
      val remaining = components.mustRemove(it, count)
      if (remaining == 0) unregisterDependencies(it)
      removeEffects(it, count)
    }
    gaining?.let {
      val newCount = components.add(it, count)
      if (newCount == count && count > 0) registerDependencies(it)
      addEffects(it, count)
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
      dependentsByDependency[removing]?.let { dependents ->
        if (dependents.isNotEmpty()) {
          throw ExistingDependentsException(dependents.map { it.type })
        }
      }
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

  internal companion object {
    internal fun empty(classTable: ClassTable): ComponentGraph =
        ComponentGraph(classTable, addEffects = { _, _ -> }, removeEffects = { _, _ -> })
  }
}
