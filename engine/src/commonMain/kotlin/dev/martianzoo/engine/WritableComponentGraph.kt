package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExistingDependentsException
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.engine.Engine.Updater
import dev.martianzoo.types.Class
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Type
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset

/** Internal mutation capability paired with the public read-only [ComponentGraph]. */
internal interface WritableComponentGraph : ComponentGraph, Updater {

  /** Component graph backed by the complete live game's component multiset. */
  class Whole(private val effector: Effector, private val classTable: ClassTable) :
      WritableComponentGraph {

    private val shardClassByClass = mutableMapOf<Class, Class>()
    private val queryShardClassesByClass = mutableMapOf<Class, Set<Class>>()
    private val components =
        ShardedMultiset<Component, Type, Class>(
            shardFor = { shardClass(it.type.rootClass) },
            queryShardsFor = { queryShardClasses(it.rootClass) },
        )

    override operator fun contains(component: Component): Boolean {
      requireOwnClassTable(component.type)
      return component in components
    }

    override fun count(parentType: Type, info: TypeInfo): Int {
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

    override fun containsAny(parentType: Type, info: TypeInfo): Boolean {
      requireOwnClassTable(parentType)
      return if (parentType.phantom) {
        false
      } else if (parentType.abstract) {
        components.queryElements(parentType).any { it.hasType(parentType, info) }
      } else {
        parentType.toComponent() in components
      }
    }

    override fun countComponent(component: Component): Int {
      requireOwnClassTable(component.type)
      return components.count(component)
    }

    override fun getAll(parentType: Type, info: TypeInfo): Multiset<Component> {
      requireOwnClassTable(parentType)
      return if (parentType.phantom) {
        HashMultiset()
      } else if (parentType.className == COMPONENT) {
        components.copy()
      } else if (parentType.abstract) {
        components.filter(parentType) { it.hasType(parentType, info) }
      } else {
        val cpt = parentType.toComponent()
        HashMultiset<Component>().also { it.add(cpt, components.count(cpt)) }
      }
    }

    override fun update(count: Int, gaining: Component?, removing: Component?): StateChange {
      listOfNotNull(gaining, removing).forEach {
        requireOwnClassTable(it.type)
        if (it.isCustom) {
          throw ExpressionException(
              "Custom component `${it.expressionFull}` cannot enter ComponentGraph"
          )
        }
      }
      removing?.let { r ->
        checkDependents(count, r)
        components.mustRemove(r, count)
        effector.mustRemove(r, count)
      }
      gaining?.let { g ->
        components.add(g, count)
        effector.add(g, count)
      }
      return StateChange(count, gaining?.expressionFull, removing?.expressionFull)
    }

    private fun requireOwnClassTable(type: Type) {
      require(type.classTable === classTable) { "$type belongs to a different class table" }
    }

    private fun queryShardClasses(klass: Class): Set<Class> =
        queryShardClassesByClass.getOrPut(klass) {
          // An abstract query can cross a later inheritance junction, so include the shard of
          // every possible root subclass. The shards partition components, so summing them is
          // safe.
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
  }
}
