package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpression

/**
 * So a TypeExpression resolves into a PetType by matching up the specs with appropriate deps.
 * of course each spec type must be a subtype of the existing from the petclass
 * But equivalent pettypes are equal regardless of how they happened to be specified.
 */
data class PetType(
    val petClass: PetClass,
    val dependencies: DependencyMap = DependencyMap()
) {
  val abstract: Boolean = petClass.abstract || dependencies.abstract

  fun isSubtypeOf(that: PetType): Boolean =
      petClass.isSubtypeOf(that.petClass) &&
      dependencies.specializes(that.dependencies)

  /**
   * Returns the common supertype of every subtype of both `this` and `that`, if possible.
   */
  fun glb(that: PetType): PetType {
    return PetType(
        petClass.glb(that.petClass),
        dependencies.merge(that.dependencies))
  }

  fun specialize(specs: List<TypeExpression>): PetType {
    return try {
      copy(dependencies = dependencies.specialize(specs, petClass.loader))
    } catch (e: RuntimeException) {
      throw RuntimeException("2. PetType is $this", e)
    }
  }

  override fun toString() = toTypeExpressionFull().toString()

  fun toTypeExpressionFull(): TypeExpression {
    val specs = dependencies.keyToDep.values.map { it.toTypeExpressionFull() }
    return TypeExpression(petClass.name, specs)
  }

  fun hasGlbWith(type: PetType): Boolean {
    return try {
      glb(type)
      true
    } catch (e: Exception) {
      false
    }
  }
}
