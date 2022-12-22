package dev.martianzoo.tfm.types

import dev.martianzoo.util.joinOrEmpty

/**
 * So a TypeExpression resolves into a PetType by matching up the specs with appropriate deps.
 * of course each spec type must be a subtype of the existing from the petclass
 * But equivalent pettypes are equal regardless of how they happened to be specified.
 */
data class PetType(val petClass: PetClass, val dependencies: DependencyMap = DependencyMap()) : DependencyTarget {
  override val abstract: Boolean = petClass.abstract || dependencies.abstract

  override fun isSubtypeOf(that: DependencyTarget): Boolean =
      petClass.isSubtypeOf((that as PetType).petClass) && dependencies.sub(that.dependencies)

  /**
   * Returns the common supertype of every subtype of both `this` and `that`, if possible.
   */
  override fun glb(that: DependencyTarget): DependencyTarget {
    if (that !is PetType) error("")
    return PetType(
        petClass.glb(that.petClass),
        dependencies.merge(that.dependencies))
  }

  fun specialize(specs: List<PetType>): PetType {
    return try {
      copy(dependencies = dependencies.specialize(specs))
    } catch (e: RuntimeException) {
      throw RuntimeException("2. PetType is $this", e)
    }
  }

  override fun toString(): String {
    val deps = dependencies.keyToType.map { "${it.key}=${it.value}" }.joinOrEmpty(", ", "<>")
    return "$petClass$deps"
  }

}
