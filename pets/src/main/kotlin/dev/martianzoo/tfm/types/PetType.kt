package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.Predicate
import dev.martianzoo.util.joinOrEmpty

/** An actual type type, like the one represented by `CityTile<LandArea>`. */
data class PetType(
    val petClass: PetClass,
    val dependencies: DependencyMap,
    val predicates: Set<Predicate> = setOf()) {
  init {
    // specializations.forEach { }
  }

  fun isSubtypeOf(other: PetType): Boolean =
      petClass.isSubclassOf(other.petClass) &&
      dependencies.specializes(other.dependencies)

  fun specialize(specs: DependencyMap): PetType {
    return copy(dependencies = dependencies.specialize(specs))
  }

  fun glb(other: PetType): PetType {
    val newClass = petClass.glb(other.petClass)
    val newDeps = DependencyMap.merge(listOf(dependencies, other.dependencies))
    val newPredicates = predicates.union(other.predicates)
    return PetType(newClass, newDeps, newPredicates).also {
      require(it.isSubtypeOf(this))
      require(it.isSubtypeOf(other))
    }
  }

  override fun toString() =
      "$petClass$dependencies${predicates.joinOrEmpty(prefix = "(HAS ", suffix = ")")}"
}
