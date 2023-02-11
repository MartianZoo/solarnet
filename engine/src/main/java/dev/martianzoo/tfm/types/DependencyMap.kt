package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.util.mergeMaps
import dev.martianzoo.util.overlayMaps

// Takes care of everything inside the <> but knows nothing of what's outside it
internal data class DependencyMap(private val map: Map<Key, Dependency>) {
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
      that.map.all { (thatKey, thatType) -> map[thatKey]!!.isSubtypeOf(thatType) }

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
      var map = DependencyMap()
      maps.forEach { map = map.intersect(it) }
      return map
    }
  }

  // determines the map that could be merged with this one to specialize, by inferring which
  // keys the provided specs go with
  fun findMatchups(specs: List<PType>): DependencyMap {
    val matchups = mutableMapOf<Key, TypeDependency>()
    val unhandled = specs.toMutableList()

    // a) every spec should have exactly one dep it could go with
    // b) every dep should have 0 or 1 specs it should go with, and check that we handled all
    for ((key, dep) in map) {
      if (unhandled.none()) break
      dep as TypeDependency
      val iter: MutableIterator<PType> = unhandled.iterator()
      while (iter.hasNext()) {
        val ptype = iter.next().intersect(dep.ptype)
        if (ptype != null) {
          matchups[key] = TypeDependency(key, ptype)
          iter.remove()
          break
        }
      }
    }
    require(matchups.size == specs.size && unhandled.isEmpty()) {
      """
      This: $this
      Specs: $specs
      Matchups: ${matchups.values}
      Unhandled : $unhandled
      """
    }
    return DependencyMap(matchups)
  }

  fun specialize(specs: List<PType>) = intersect(findMatchups(specs))

  override fun toString() = "$types"
}
