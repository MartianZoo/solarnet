package dev.martianzoo.tfm.types

import dev.martianzoo.util.mergeMaps
import dev.martianzoo.util.overlayMaps

// Takes care of everything inside the <> but knows nothing of what's outside it
public data class DependencyMap(private val map: Map<Dependency.Key, Dependency>) {
  // TODO make that private?
  internal constructor() : this(mapOf<Dependency.Key, Dependency>())

  init {
    map.forEach { (key, dep) -> require(key == dep.key) { key } }
  }

  public val keys by map::keys
  public val types by map::values
  public fun isEmpty() = map.isEmpty()

  public val abstract = types.any { it.abstract }

  public operator fun contains(key: Dependency.Key) = key in map
  public operator fun get(key: Dependency.Key): Dependency? = map[key]

  public fun specializes(that: DependencyMap) =
      // For each of *its* keys, my type must be a subtype of its type
      that.map.all { (thatKey, thatType) -> map[thatKey]!!.specializes(thatType) }

  // Combines all entries, using the glb when both maps have the same key
  public fun intersect(that: DependencyMap): DependencyMap {
    val merged = mergeMaps(this.map, that.map) { a, b -> a.intersect(b)!! }
    return DependencyMap(merged)
  }

  public fun overlayOn(that: DependencyMap) = DependencyMap(overlayMaps(this.map, that.map))

  companion object {
    public fun intersect(maps: Collection<DependencyMap>): DependencyMap {
      // TODO improve, watch out for order
      var map = DependencyMap()
      maps.forEach { map = map.intersect(it) }
      return map
    }
  }

  // determines the map that could be merged with this one to specialize, by inferring which
  // keys the provided specs go with
  // TODO fix the strict-order problem!
  public fun findMatchups(specs: List<PType>): DependencyMap {
    if (specs.isEmpty()) return this
    val newMap = mutableMapOf<Dependency.Key, Dependency>()
    val unhandled = specs.toMutableList()

    for ((key, dependency) in map) {
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

  public fun specialize(specs: List<PType>) = intersect(findMatchups(specs))

  override fun toString() = "$types"
}
