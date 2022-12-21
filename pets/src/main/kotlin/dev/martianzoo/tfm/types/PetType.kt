package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.types.PetType.DependencyKey
import dev.martianzoo.util.merge

/**
 * So a TypeExpression resolves into a PetType by matching up the specs with appropriate deps.
 * of course each spec type must be a subtype of the existing from the petclass
 * But equivalent pettypes are equal regardless of how they happened to be specified.
 */
data class PetType(val petClass: PetClass, val dependencies: DependencyMap) {
  val abstract: Boolean = petClass.abstract || dependencies.abstract

  fun isSubtypeOf(that: PetType): Boolean =
      petClass.isSubclassOf(that.petClass) && dependencies.sub(that.dependencies)

  /**
   * Returns the common supertype of every subtype of both `this` and `that`, if possible.
   */
  fun glb(that: PetType): PetType = copy(
      petClass.glb(that.petClass),
      dependencies.merge(that.dependencies)
  )

  fun specialize(specs: List<PetType>): PetType {
    return copy(dependencies = dependencies.specialize(specs))
  }

  data class DependencyMap(val keyToType: Map<DependencyKey, PetType>) {
    val abstract = keyToType.values.any { it.abstract }

    fun sub(that: DependencyMap) =
        // For each of *its* keys, my type must be a subtype of its type
        that.keyToType.all { (thatKey, thatType) -> keyToType[thatKey]!!.isSubtypeOf(thatType) }

    // Combines all entries, using the glb when both maps have the same key
    fun merge(that: DependencyMap) =
        DependencyMap(merge(this.keyToType, that.keyToType)  { type1, type2 -> type1.glb(type2) })

    // determines the map that could be merged with this one to specialize
    //fun findMatchups(specs: List<PetType>): DependencyMap {
    //}

    fun specialize(specs: List<PetType>): DependencyMap {
      val unhandled = specs.toMutableList()
      val newMap: Map<DependencyKey, PetType> = keyToType.map { (key, originalType) ->
        val newType = unhandled.firstOrNull { it.isSubtypeOf(originalType) } ?: originalType
        unhandled.remove(newType) // does nothing if `originalType`
        key to newType
      }.toMap()
      require (unhandled.isEmpty()) { "Unrecognized specializations: $unhandled"}
      return copy(keyToType = newMap)
    }
  }

  data class DependencyKey(val declaringClass: PetClass, val index: Int) {
    init { require(index >= 0) }
    override fun toString() = "${declaringClass.name}_$index"
  }
}

private fun merge(one: Map<DependencyKey, PetType>, two: Map<DependencyKey, PetType>) =
    merge(one, two)  { type1, type2 -> type1.glb(type2) }

