package dev.martianzoo.tfm.types

import dev.martianzoo.util.joinOrEmpty
import java.util.*

data class DependencyMap(val map: Map<DependencyKey, PetType> = mapOf()) {
  fun specializes(dependencies: DependencyMap) =
      dependencies.map.all { (k, v) -> map[k]!!.isSubtypeOf(v) }

  /**
   * TODO Stricter than necessary; insists on exact order
   */
  fun specialize(specializations: List<PetType>): DependencyMap {
    if (specializations.isEmpty()) {
      return this
    }
    val unhandled: Queue<PetType> = ArrayDeque(specializations)
    val newMap = mutableMapOf<DependencyKey, PetType>()
    map.forEach { (key, depType) ->
      val nextType = unhandled.peek()
      if (nextType != null && nextType.isSubtypeOf(depType)) {
        newMap.put(key, nextType);
        unhandled.poll()
      } else {
        newMap.put(key, depType)
      }
    }
    require(unhandled.isEmpty()) { unhandled }
    return DependencyMap(newMap)
  }

  fun specialize(other: DependencyMap): DependencyMap {
    val newMap = map.toMutableMap()
    other.map.forEach { (key, specialType) ->
      val previousType = map[key] ?: error("expected to find all of ${other.map} in $map")
      require(specialType.isSubtypeOf(previousType))
      newMap[key] = specialType
    }
    return DependencyMap(newMap)
  }

  override fun toString() =
      map.map { "${it.key}=${it.value}" }.joinOrEmpty(surround = "<>")

  companion object {
    fun merge(maps: List<DependencyMap>): DependencyMap {
      val result = mutableMapOf<DependencyKey, PetType>()
      maps.forEach {
        depMap -> depMap.map.forEach {
          result.merge(it.key, it.value, PetType::glb)
        }
      }
      return DependencyMap(result)
    }
  }

  // ("Owned", table.resolve("Anyone"))
  // ("Tile", table.resolve("Area"))
  // ("Production", table.resolve("StandardResource"), true)
  // ("Adjacency", table.resolve("Tile"), 0)
  // ("Adjacency", table.resolve("Tile"), 1)
  data class DependencyKey(
      val dependentTypeName: String,
      val index: Int = 0,
  ) {
    override fun toString() = "${dependentTypeName}_$index"
  }
}
