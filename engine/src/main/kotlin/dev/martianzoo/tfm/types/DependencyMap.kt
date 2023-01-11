package dev.martianzoo.tfm.types

import dev.martianzoo.util.mergeMaps
import dev.martianzoo.util.overlayMaps

// Takes care of everything inside the <> but knows nothing of what's outside it
internal data class DependencyMap(val keyToDependency: Map<Dependency.Key, Dependency>) {

  constructor() : this(mapOf<Dependency.Key, Dependency>())

  init {
    keyToDependency.forEach { (key, dep) ->
      require(key == dep.key) { key }
    }
  }

  val abstract = keyToDependency.values.any { it.abstract }

  val keys = keyToDependency.keys

  operator fun contains(key: Dependency.Key) = key in keyToDependency
  operator fun get(key: Dependency.Key): Dependency = keyToDependency[key]!!

  fun specializes(that: DependencyMap) =
      // For each of *its* keys, my type must be a subtype of its type
      that.keyToDependency.all { (thatKey, thatType) ->
        keyToDependency[thatKey]!!.specializes(thatType)
      }

  // Combines all entries, using the glb when both maps have the same key
  fun intersect(that: DependencyMap): DependencyMap {
    val merged = mergeMaps(this.keyToDependency, that.keyToDependency, Dependency::intersect)
    return DependencyMap(merged)
  }

  fun overlayOn(that: DependencyMap) =
      DependencyMap(overlayMaps(this.keyToDependency, that.keyToDependency))

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
  fun findMatchups(specs: List<PetType>): DependencyMap {
    if (specs.isEmpty()) return this
    val newMap = mutableMapOf<Dependency.Key, Dependency>()
    val unhandled = specs.toMutableList()

    for ((key, dependency) in keyToDependency) {
      if (unhandled.isEmpty()) break
      newMap[key] = if (dependency.type.canIntersect(unhandled.first())) {
        dependency intersect unhandled.removeFirst()
      } else {
        dependency
      }
    }
    require(unhandled.isEmpty()) {
      "This: $this\nSpecs: $specs\nUnhandled : $unhandled"
    }
    return DependencyMap(newMap) //.d { "findMatchups of $this with $specs: $it" } too noisy
  }

  fun specialize(specs: List<PetType>) = intersect(findMatchups(specs))

  override fun toString() = "${keyToDependency.values}"
}
