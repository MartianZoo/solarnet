package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.types.CTypeClass.DependencyKey
import dev.martianzoo.util.joinOrEmpty
import java.util.*

data class DependencyMap(val map: Map<DependencyKey, CType>) {
  fun specializes(dependencies: DependencyMap) =
      dependencies.map.all { (k, v) -> map[k]!!.isSubtypeOf(v) }

  fun specialize(specializations: List<CType>): DependencyMap {
    val unhandled: Queue<CType> = ArrayDeque(specializations)
    val newMap = mutableMapOf<DependencyKey, CType>()
    val specs = mutableMapOf<DependencyKey, CType>()

    for ((dep, type) in map) {
      val peeked = unhandled.peek()
      if (peeked?.isSubtypeOf(type) == true) {
        newMap[dep] = peeked
        specs[dep] = peeked
        unhandled.poll() // remove `peeked` from queue
      } else {
        newMap[dep] = type.specialize(DependencyMap(specs))
      }
    }
    require(unhandled.isEmpty()) { "$this\n\n$specializations\n\n$unhandled" }
    return DependencyMap(newMap)
  }

  fun specialize(other: DependencyMap): DependencyMap {
    val newMap = map.toMutableMap()
    other.map.forEach {
      val existing = newMap[it.key]!!
      require(it.value.isSubtypeOf(existing))
      newMap[it.key] = it.value
    }
    return DependencyMap(newMap)
  }

  override fun toString() =
      map.map { "${it.key}=${it.value}" }.joinOrEmpty(surround = "<>")

  companion object {
    fun merge(maps: List<DependencyMap>): DependencyMap {
      val result = mutableMapOf<DependencyKey, CType>()
      maps.forEach {
        depMap -> depMap.map.forEach {
          result.merge(it.key, it.value, CType::glb)
        }
      }
      return DependencyMap(result)
    }
  }
}
