package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExistingDependentsException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.types.Class
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Type
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset

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
   * zero for a phantom type, which cannot have stored components.
   */
  internal fun count(parentType: Type, info: TypeInfo): Int {
    requireOwnClassTable(parentType)
    return if (parentType.phantom) {
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
    return if (parentType.phantom) {
      false
    } else if (parentType.abstract) {
      components.queryElements(parentType).any { it.hasType(parentType, info) }
    } else {
      parentType.toComponent() in components
    }
  }

  /**
   * Returns all component instances having the type [parentType] (or any of its subtypes), as a
   * multiset. The size of the returned collection will be `[count]([parentType])` . A phantom type
   * returns an empty multiset. If [parentType] is `Component` this returns the entire component
   * multiset.
   */
  internal fun getAll(parentType: Type, info: TypeInfo): Multiset<Component> {
    requireOwnClassTable(parentType)
    return if (parentType.phantom) {
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
      if (it.isCustom) {
        throw ExpressionException(
            "Custom component `${it.expressionFull}` cannot enter ComponentGraph"
        )
      }
    }
    removing?.let {
      checkDependents(count, it)
      components.mustRemove(it, count)
      removeEffects(it, count)
    }
    gaining?.let {
      components.add(it, count)
      addEffects(it, count)
    }
  }

  private fun requireOwnClassTable(type: Type) {
    require(type.classTable === classTable) { "$type belongs to a different class table" }
  }

  private fun queryShardClasses(klass: Class): Set<Class> =
      queryShardClassesByClass.getOrPut(klass) {
        // An abstract query can cross a later inheritance junction, so include the shard of every
        // possible root subclass. The shards partition components, so summing them is safe.
        klass.allSubclasses().mapTo(linkedSetOf(), ::shardClass)
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
      val dependents =
          components.distinctElements().filter { removing in it.dependencyComponents }.toList()
      if (dependents.isNotEmpty()) {
        throw ExistingDependentsException(dependents.map { it.type })
      }
    }
  }

  internal companion object {
    internal fun empty(classTable: ClassTable): ComponentGraph =
        ComponentGraph(classTable, addEffects = { _, _ -> }, removeEffects = { _, _ -> })
  }
}
