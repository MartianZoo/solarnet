package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.types.PetType.DependencyKey

/**
 * So a TypeExpression resolves into a PetType by matching up the specs with appropriate deps.
 * of course each spec type must be a subtype of the existing from the petclass
 * But equivalent pettypes are equal regardless of how they happened to be specified.
 */
data class PetType(val petClass: PetClass, val deps: Map<DependencyKey, PetType>) { // TODO: pred

  fun glb(other: PetType): PetType {
    val clazz = petClass.glb(other.petClass)
    val deps = merge(this.deps, other.deps)
    return PetType(clazz, deps)
  }

  fun isSubtypeOf(other: PetType): Boolean {
    return petClass.isSubclassOf(other.petClass) &&
        other.deps.entries.all {
          (superKey, superDepType) -> this.deps[superKey]!!.isSubtypeOf(superDepType)
        }
  }

  fun specialize(specs: Map<DependencyKey, PetType>) = copy(deps = merge(deps, specs))

  fun specialize(specs: List<PetType>): PetType {
    val unhandled = specs.toMutableList()
    val list: List<Pair<DependencyKey, PetType>> = deps.entries.map { (k, v) ->
      val cand = unhandled.firstOrNull { it.isSubtypeOf(v) }
      k to if (cand != null) {
        unhandled.remove(cand)
        cand
      } else {
        v
      }
    }
    return copy(deps = list.toMap())
  }

  data class DependencyKey(val declaringClass: PetClass, val index: Int) {
    init { require(index >= 0) }
    override fun toString() = "${declaringClass.name}_$index"
  }
}

private fun merge(one: Map<DependencyKey, PetType>, two: Map<DependencyKey, PetType>): Map<DependencyKey, PetType> {
  val map = one.toMutableMap()
  for ((key, type) in two.entries) {
    map.merge(key, type)  { type1, type2 -> type1.glb(type2) }
  }
  return map
}


