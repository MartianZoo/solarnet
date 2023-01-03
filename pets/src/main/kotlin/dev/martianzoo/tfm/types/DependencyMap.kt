package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.mergeMaps

// Takes care of everything inside the <> but knows nothing of what's outside it
data class DependencyMap(val keyToDep: Map<DependencyKey, Dependency>) {

  constructor() : this(mapOf<DependencyKey, Dependency>())

  init {
    keyToDep.forEach { (key, dep) ->
      require(key == dep.key) { key }
    }
  }
  val abstract = keyToDep.values.any { it.abstract }

  val keys = keyToDep.keys

  operator fun contains(key: DependencyKey) = key in keyToDep
  operator fun get(key: DependencyKey): Dependency = keyToDep[key]!!

  fun specializes(that: DependencyMap) =
      // For each of *its* keys, my type must be a subtype of its type
      that.keyToDep.all { (thatKey, thatType) -> keyToDep[thatKey]!!.specializes(thatType) }

  // Combines all entries, using the glb when both maps have the same key
  fun merge(that: DependencyMap) =
      DependencyMap(mergeMaps(this.keyToDep, that.keyToDep, Dependency::combine)
  )

  fun overlayOn(that: DependencyMap): DependencyMap {
    val map = that.keyToDep.toMutableMap()
    map.putAll(keyToDep)
    return DependencyMap(map)
  }

  companion object {
    fun merge(maps: Collection<DependencyMap>): DependencyMap {
      var map = DependencyMap()
      maps.forEach { map = map.merge(it) }
      return map
    }
  }

  // determines the map that could be merged with this one to specialize, by inferring which
  // keys the provided specs go with
  fun findMatchups(specs: List<TypeExpression>, loader: PetClassLoader): DependencyMap {
    if (specs.isEmpty()) return DependencyMap()

    val unhandled = specs.toMutableList()
    val newMap = keyToDep.mapValues { (key, dep) ->
      if (unhandled.isNotEmpty() && dep.acceptsSpecialization(unhandled.first(), loader)) {
        dep.specialize(unhandled.removeFirst(), loader)
      } else {
        dep
      }
    }
    require(unhandled.isEmpty()) { "This: $this\nSpecs: $specs\nUnhandled : $unhandled" }
    return DependencyMap(newMap)
  }

  fun specialize(specs: List<TypeExpression>, loader: PetClassLoader) = merge(findMatchups(specs, loader))

  override fun toString() = "${keyToDep.values}"
}
