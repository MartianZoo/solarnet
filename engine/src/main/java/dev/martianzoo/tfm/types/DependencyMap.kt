package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.associateByStrict
import dev.martianzoo.util.mergeMaps
import dev.martianzoo.util.toSetStrict

// Takes care of everything inside the <> but knows nothing of what's outside it
// TODO make this a list
internal data class DependencyMap(val list: List<Dependency>) {
  constructor() : this(listOf<Dependency>())

  val keys: List<Key> = list.map { it.key }.toSetStrict().toList() // TODO

  val abstract = list.any { it.abstract }

  fun get(key: Key): Dependency = getIfPresent(key) ?: error("$key")
  fun getIfPresent(key: Key): Dependency? = list.firstOrNull { it.key == key }

  // used by PType.isSubtypeOf()
  fun specializes(that: DependencyMap) =
      that.list.all { thatDep: Dependency -> this.get(thatDep.key).isSubtypeOf(thatDep) }

  private fun asMap() = list.associateByStrict { it.key } // TODO get rid

  fun merge(that: DependencyMap, merger: (Dependency, Dependency) -> Dependency) =
      copy(mergeMaps(asMap(), that.asMap(), merger).values.toList()) // TODO fix

  // Combines all entries, using the glb when both maps have the same key
  fun intersect(that: DependencyMap) = merge(that) { a, b -> a.intersect(b)!! }

  fun lub(that: DependencyMap): DependencyMap {
    val keys = keys.intersect(that.keys)
    return copy(keys.map { get(it).lub(that.get(it))!! })
  }

  fun overlayOn(that: DependencyMap) = merge(that) { ours, _ -> ours }

  fun minus(that: DependencyMap) = copy(this.list - that.list)

  companion object {
    fun intersect(maps: Collection<DependencyMap>): DependencyMap {
      var map = DependencyMap()
      maps.forEach { map = map.intersect(it) }
      return map
    }
  }

  /**
   * Assigns each expression to a key from among this map's keys, such that it is compatible
   * with that key's upper bound.
   */
  fun match(specs: List<Expression>, loader: PClassLoader): List<TypeDependency> {
    val usedDeps = mutableSetOf<TypeDependency>()

    return specs.map { specExpression ->
      val specType: PType = loader.resolve(specExpression)
      for (candidateDep in list - usedDeps) {
        candidateDep as TypeDependency
        val intersectionType = specType.intersect(candidateDep.bound) ?: continue
        usedDeps += candidateDep
        return@map TypeDependency(candidateDep.key, intersectionType)
      }
      error("couldn't match up $specExpression to $this")
    }
  }

  fun specialize(specs: List<Expression>, loader: PClassLoader): DependencyMap {
    return copy(match(specs, loader)).overlayOn(this)
  }

  override fun toString() = "$list"

  /** Returns a submap of this map where every key is one of [keysInOrder]. */
  fun subMap(keysInOrder: Iterable<Key>) = copy(keysInOrder.mapNotNull(::getIfPresent))
}
