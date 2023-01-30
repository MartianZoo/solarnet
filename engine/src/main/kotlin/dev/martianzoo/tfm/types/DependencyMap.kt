package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.mergeMaps
import dev.martianzoo.util.overlayMaps

// Takes care of everything inside the <> but knows nothing of what's outside it
internal data class DependencyMap(private val map: Map<Key, Dependency>) {
  // TODO make that private?
  internal constructor() : this(mapOf<Key, Dependency>())

  constructor(vararg pairs: Pair<Key, Dependency>) : this(mapOf(*pairs))

  init {
    map.forEach { (key, dep) -> require(key == dep.key) { key } }
  }

  val keys by map::keys
  val types by map::values
  fun isEmpty() = map.isEmpty()

  val abstract = types.any { it.abstract }

  operator fun contains(key: Key) = key in map
  operator fun get(key: Key): Dependency? = map[key]

  fun specializes(that: DependencyMap) =
      // For each of *its* keys, my type must be a subtype of its type
      that.map.all { (thatKey, thatType) -> map[thatKey]!!.specializes(thatType) }

  // Combines all entries, using the glb when both maps have the same key
  fun intersect(that: DependencyMap): DependencyMap {
    val merged = mergeMaps(this.map, that.map) { a, b -> a.intersect(b)!! }
    return DependencyMap(merged)
  }

  fun overlayOn(that: DependencyMap) = DependencyMap(overlayMaps(this.map, that.map))

  fun minus(that: DependencyMap) =
      DependencyMap((map.entries - that.map.entries).associate { it.key to it.value })

  companion object {
    fun intersect(maps: Collection<DependencyMap>): DependencyMap {
      // TODO improve, watch out for order
      var map = DependencyMap()
      maps.forEach { map = map.intersect(it) }
      return map
    }
  }

  // determines the map that could be merged with this one to specialize, by inferring which
  // keys the provided specs go with
  // TODO fix the strict-order problem!
  fun findMatchups(specs: List<PType>): DependencyMap {
    if (specs.isEmpty()) return this

    val newMap = mutableMapOf<Key, Dependency>()
    val unhandled = specs.toMutableList()

    for ((key, dependency) in map) {
      dependency as TypeDependency
      if (unhandled.isEmpty()) break
      val intersect: PType? = dependency.ptype.intersect(unhandled.first())
      newMap[key] =
          if (intersect != null) {
            unhandled.removeFirst()
            dependency.copy(ptype = intersect)
          } else {
            dependency
          }
    }
    require(unhandled.isEmpty()) { "This: $this\nSpecs: $specs\nUnhandled : $unhandled" }
    return DependencyMap(newMap)
  }

  fun specialize(specs: List<PType>) = intersect(findMatchups(specs))

  override fun toString() = "$types"

  fun argsAsTypeExprs() = types.map { it.toTypeExprFull() } // TODO not full?
}
