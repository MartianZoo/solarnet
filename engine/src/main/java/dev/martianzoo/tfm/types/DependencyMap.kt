package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.mergeMaps

// Takes care of everything inside the <> but knows nothing of what's outside it
internal data class DependencyMap(internal val map: Map<Key, Dependency>) {
  constructor() : this(mapOf<Key, Dependency>())
  constructor(deps: Collection<Dependency>) : this(deps.associateBy { it.key })

  init {
    map.forEach { (key, dep) -> require(key == dep.key) { key } }
  }

  val keys: Set<Key> by map::keys
  val dependencies: List<Dependency> = map.values.toList()

  val abstract = dependencies.any { it.abstract }

  operator fun contains(key: Key) = key in map
  operator fun get(key: Key): Dependency? = map[key] // TODO throw if not?

  // used by PType.isSubtypeOf()
  fun specializes(that: DependencyMap) =
      // For each of *its* keys, my type must be a subtype of its type
      that.map.all { (thatKey, thatType: Dependency) -> map[thatKey]!!.isSubtypeOf(thatType) }

  fun merge(that: DependencyMap, merger: (Dependency, Dependency) -> Dependency) =
      DependencyMap(mergeMaps(map, that.map, merger))

  // Combines all entries, using the glb when both maps have the same key
  fun intersect(that: DependencyMap) = merge(that) { a, b -> a.intersect(b)!! }

  fun lub(that: DependencyMap): DependencyMap {
    val keys = map.keys.intersect(that.map.keys)
    return DependencyMap(keys.map { this[it]?.lub(that[it])!! })
  }

  fun overlayOn(that: DependencyMap) = merge(that) { ours, _ -> ours }

  operator fun minus(that: DependencyMap) =
      DependencyMap((map.entries - that.map.entries).associate { it.key to it.value })

  companion object {
    fun intersect(maps: Collection<DependencyMap>): DependencyMap {
      var map = DependencyMap()
      maps.forEach { map = map.intersect(it) }
      return map
    }
  }

  /**
   * Assigns each type expression to a key from among this map's keys, such that it is compatible
   * with that key's upper bound.
   */
  fun match(specs: List<TypeExpr>, loader: PClassLoader): List<TypeDependency> {
    val usedDeps = mutableSetOf<TypeDependency>()

    return specs.map { specTypeExpr ->
      val specType: PType = loader.resolveType(specTypeExpr)
      for (candidateDep in dependencies - usedDeps) {
        candidateDep as TypeDependency
        val intersectionType = specType.intersect(candidateDep.ptype) ?: continue
        usedDeps += candidateDep
        return@map TypeDependency(candidateDep.key, intersectionType)
      }
      error("couldn't match up $specTypeExpr to $this")
    }
  }

  fun specialize(specs: List<TypeExpr>, loader: PClassLoader): DependencyMap {
    return DependencyMap(match(specs, loader)).overlayOn(this)
  }

  override fun toString() = "$dependencies"

  /** Returns a submap of this map where every key is one of [keysInOrder]. */
  fun subMap(keysInOrder: Iterable<Key>): DependencyMap {
    val map = mutableMapOf<Key, Dependency>()
    keysInOrder.forEach {
      if (it in this.map) {
        map[it] = this.map[it]!!
      }
    }
    return DependencyMap(map)
  }
}
